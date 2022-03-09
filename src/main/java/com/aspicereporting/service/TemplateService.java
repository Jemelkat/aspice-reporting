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

    public List<Template> getAllByUser(User user) {
        return templateRepository.findAllByTemplateUser(user);
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

        //Update name and orientation
        oldTemplate.setTemplateName(updatedTemplate.getTemplateName());
        oldTemplate.setOrientation(updatedTemplate.getOrientation());

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
                Long sourceId = tableItem.getSource().getId();
                //Validate - user can use this source id
                Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
                if (source == null) {
                    throw new EntityNotFoundException("You dont have any source with id=" + sourceId);
                }

                //Table columns are reinserted every time - not updated
                tableItem.getTableColumns().forEach(tableColumn -> {
                    //Validate if column exists in source
                    Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> c.getId() == tableColumn.getSourceColumn().getId()).findFirst();
                    if (columnExists.isEmpty()) {
                        throw new EntityNotFoundException("Invalid source column id=" + tableColumn.getSourceColumn().getId() + " for source id=" + sourceId);
                    }
                    tableColumn.setId(null);
                    //Bidirectional
                    //tableColumn.setSimpleTable(tableItem);
                });
            }
            reportItem.setTemplate(oldTemplate);
            newTemplateItems.add(reportItem);
        }
        oldTemplate.getTemplateItems().clear();
        oldTemplate.getTemplateItems().addAll(newTemplateItems);

        return templateRepository.save(oldTemplate);
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
