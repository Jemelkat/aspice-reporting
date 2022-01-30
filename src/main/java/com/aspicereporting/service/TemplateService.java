package com.aspicereporting.service;

import com.aspicereporting.entity.*;
import com.aspicereporting.entity.items.*;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.exception.UnauthorizedAccessException;
import com.aspicereporting.repository.SourceRepository;
import com.aspicereporting.repository.TemplateRepository;
import com.aspicereporting.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Table;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TemplateService {

    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    SourceRepository sourceRepository;

    @Autowired
    UserGroupRepository userGroupRepository;

    public List<Template> getAllByUserOrShared(User user) {
        return templateRepository.findDistinctByTemplateUserOrTemplateGroupsIn(user, user.getUserGroups());
    }

    public Template getTemplateById(Long id, User user) {
        Template test = templateRepository.findByTemplateUserAndId(user, id);
        return test;
    }

    public Template saveOrEditTemplate(Template updatedTemplate, User user) {
        Template oldTemplate;
        Date changeDate = new Date();
        //Update
        if (updatedTemplate.getId() != null) {
            oldTemplate = getTemplateById(updatedTemplate.getId(), user);
            if (oldTemplate == null) {
                throw new EntityNotFoundException("Template " + updatedTemplate.getTemplateName() + " id=" + updatedTemplate.getId() + " was not found and cannot be saved.");
            }
            oldTemplate.setTemplateLastUpdated(changeDate);
        }
        //Create
        else {
            oldTemplate = updatedTemplate;
            oldTemplate.setId(null);
            oldTemplate.setTemplateCreated(changeDate);
            oldTemplate.setTemplateUser(user);
        }

        //Update name and last changed time
        oldTemplate.setTemplateName(updatedTemplate.getTemplateName());

        //Configure item IDs - if they exist use same ID - hibernate will MERGE
        List<ReportItem> newTemplateItems = new ArrayList<>();
        for (ReportItem reportItem : updatedTemplate.getTemplateItems()) {
            Optional<ReportItem> existingItem = Optional.empty();
            if (oldTemplate.getId() != null) {
                existingItem = oldTemplate.getTemplateItems().stream()
                        .filter(i -> i.getId().equals(reportItem.getId()))
                        .findAny();
            }
            //If item with this ID does not exist - we will create new record in DB
            if (existingItem.isEmpty()) {
                reportItem.setId(null);
            }

            //Configure and reconstruct relationship items IDs
            if (reportItem instanceof TextItem textItem) {
                //If this item ID was not found or its instance is not TextItem
                if (reportItem.getId() == null || !(existingItem.get() instanceof TextItem)) {
                    textItem.getTextStyle().setId(null);
                } else {
                    //Use existing textStyle ID
                    textItem.getTextStyle().setId(((TextItem) existingItem.get()).getTextStyle().getId());
                }
                //Bidirectional
                textItem.getTextStyle().setTextItem(textItem);
            } else if (reportItem instanceof TableItem tableItem) {
                //Validate - all source columns need to be the same
                Long firstId = tableItem.getTableColumns().get(0).getSource().getId();
                List<TableColumn> sameSourceColumns = tableItem.getTableColumns().stream().takeWhile((i -> firstId == i.getSource().getId())).collect(Collectors.toList());
                if (sameSourceColumns.size() != tableItem.getTableColumns().size()) {
                    throw new InvalidDataException("Simple table accepts only columns from one data source!");
                }
                //Validate - user can use this source id
                Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(firstId, user, user.getUserGroups());
                if (source == null) {
                    throw new EntityNotFoundException("You dont have any source with id=" + firstId);
                }

                //Table columns are reinserted every time - not updated
                tableItem.getTableColumns().forEach(tableColumn -> {
                    //Validate if column exists in source
                    Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> c.getId() == tableColumn.getSourceColumn().getId()).findFirst();
                    if (columnExists.isEmpty()) {
                        throw new EntityNotFoundException("Invalid source column id=" + tableColumn.getSourceColumn().getId() + " for source id=" + tableColumn.getSource().getId());
                    }
                    tableColumn.setId(null);
                    //Bidirectional
                    tableColumn.setSimpleTable(tableItem);
                });
            }
            reportItem.setTemplate(oldTemplate);
            newTemplateItems.add(reportItem);
        }
        oldTemplate.getTemplateItems().clear();
        oldTemplate.getTemplateItems().addAll(newTemplateItems);

        return templateRepository.save(oldTemplate);
    }

    public void shareWithGroups(Long templateId, List<Long> groupIds, User user) {
        Template template = templateRepository.findByTemplateUserAndId(user, templateId);
        if (template == null) {
            throw new EntityNotFoundException("Could not find template with id = " + template.getId());
        }

        //Get all groups for update
        List<UserGroup> templateGroupList = userGroupRepository.findAllByIdIn(groupIds);

        //Get all removed groups
        Set<UserGroup> removedGroups = new HashSet<>(template.getTemplateGroups());
        removedGroups.removeAll(templateGroupList);

        //Remove removed groups
        for (UserGroup group : removedGroups) {
            template.removeGroup(group);
        }
        //Add new groups
        for (UserGroup group : templateGroupList) {
            template.addGroup(group);
        }

        templateRepository.save(template);
    }

    public Set<UserGroup> getGroupsForTemplate(Long templateId, User loggedUser) {
        Template template = templateRepository.findFirstById(templateId);
        if (template == null) {
            throw new EntityNotFoundException("Could not find template with id = " + templateId);
        }
        if (template.getTemplateUser().getId() != loggedUser.getId()) {
            throw new UnauthorizedAccessException("Only the owner of this template can share it.");
        }

        return template.getTemplateGroups();
    }

    public void deleteTemplate(Long templateId, User user) {
        Template template = templateRepository.findByTemplateUserAndId(user, templateId);
        if (template == null) {
            throw new EntityNotFoundException("Could not find template with id =" + templateId);
        }

        //Remove foreign key in reports
        for (Report r : template.getReports()) {
            r.setReportTemplate(null);
        }

        templateRepository.delete(template);
    }
}
