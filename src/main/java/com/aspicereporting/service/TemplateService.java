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

    @Transactional
    public Template saveOrEditTemplate(Template template, User user) {
        Template newTemplate;
        Date changeDate = new Date();
        //Edit existing template
        if (template.getId() != null) {
            //Get template if ID is defined - only templates belonging to this user can be changed
            newTemplate = getTemplateById(template.getId(), user);
            if (newTemplate == null) {
                throw new EntityNotFoundException("Template " + template.getTemplateName() + " id=" + template.getId() + " was not found and cannot be saved.");
            }

            newTemplate.setTemplateName(template.getTemplateName());
            newTemplate.setTemplateLastUpdated(changeDate);

            //Configure item IDs - if they exist in current report or not
            List<ReportItem> newTemplateItems = new ArrayList<>();
            for (ReportItem reportItem : template.getTemplateItems()) {
                Optional<ReportItem> existingItem = newTemplate.getTemplateItems().stream()
                        .filter(i -> i.getId().equals(reportItem.getId()))
                        .findAny();
                //If item with this ID does not exist - we will create new record in DB
                if (existingItem.isEmpty()) {
                    reportItem.setId(null);
                }

                //Change other ID to null if they did not exist before
                if (reportItem instanceof TextItem textItem) {
                    Optional<ReportItem> existingStyle = newTemplate.getTemplateItems().stream()
                            .filter(i -> ((TextItem) i).getTextStyle().getId().equals(textItem.getTextStyle().getId()))
                            .findAny();
                    if (existingStyle.isEmpty()) {
                        textItem.getTextStyle().setId(null);
                    }
                } else if (reportItem instanceof TableItem tableItem) {
                    handleTableColumnIdsOnUpdate(newTemplate, tableItem.getTableColumns());
                } else if (reportItem instanceof CapabilityTable capabilityTable) {
                    handleTableColumnIdsOnUpdate(newTemplate, capabilityTable.getTableColumns());
                }

                //Add the correct item
                reportItem.setReport(null);
                newTemplateItems.add(reportItem);
            }
            //Add all new items to list
            newTemplate.getTemplateItems().clear();
            newTemplate.getTemplateItems().addAll(newTemplateItems);
        }
        //Create new template
        else {
            newTemplate = template;
            newTemplate.setTemplateCreated(changeDate);
            newTemplate.setTemplateUser(user);

            //Remove ids from items and text style - Will create new items in DB
            for (ReportItem item : newTemplate.getTemplateItems()) {
                item.setId(null);
                item.setReport(null);
                if (item instanceof TextItem textItem && textItem.getTextStyle() != null) {
                    textItem.getTextStyle().setId(null);
                }
                if (item instanceof TableItem tableItem) {
                    for (TableColumn tableColumn : tableItem.getTableColumns()) {
                        tableColumn.setId(null);
                    }
                }
                if (item instanceof CapabilityTable capabilityTable) {
                    for (TableColumn tableColumn : capabilityTable.getTableColumns()) {
                        tableColumn.setId(null);
                    }
                }
            }
        }

        //Reconstruct all relationships
        for (ReportItem item : newTemplate.getTemplateItems()) {
            item.setTemplate(newTemplate);
            //ADD TEXT STYLE TO TEXT ITEM
            if (item instanceof TextItem textItem) {
                //TODO improve - new text style is created every time
                if (textItem.getTextStyle() != null && textItem.getTextStyle().isFilled()) {
                    textItem.addTextStyle(textItem.getTextStyle());
                } else {
                    textItem.setTextStyle(null);
                }

            }
            //ADD TABLE COLUMNS TO TABLES - validate if user can access this data sources
            else if (item instanceof TableItem tableItem) {
                if (!tableItem.getTableColumns().isEmpty()) {
                    Long firstId = tableItem.getTableColumns().get(0).getSource().getId();
                    List<TableColumn> sameSourceColumns = tableItem.getTableColumns().stream().takeWhile((i -> firstId == i.getSource().getId())).collect(Collectors.toList());
                    if (sameSourceColumns.size() != tableItem.getTableColumns().size()) {
                        throw new InvalidDataException("Simple table accepts only columns from one data source!");
                    }
                }
                //Add and validate each table column
                for (TableColumn tableColumn : new ArrayList<>(tableItem.getTableColumns())) {
                    tableColumn.addSimpleTable(tableItem);
                    //Check if user is the owner of this source
//                    Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(tableColumn.getSource().getId(), user, user.getUserGroups());
//                    if (source == null) {
//                        throw new EntityNotFoundException("You dont have any source with id=" + tableColumn.getSource().getId());
//                    }
//                    Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> c.getId() == tableColumn.getSourceColumn().getId()).findFirst();
//                    if (columnExists.isEmpty()) {
//                        throw new EntityNotFoundException("Invalid source column id=" + tableColumn.getSourceColumn().getId() + " for source id=" + tableColumn.getSource().getId());
//                    }
//                    tableColumn.addSource(source);
                    //tableColumn.addSourceColumn(columnExists.get());
                }
            }
        }
        return templateRepository.save(newTemplate);
    }

    @Transactional
    public Template editExistingTemplate(Template template, User user) {
        Template oldTemplate;
        Date changeDate = new Date();

        //Get template if ID is defined - only templates belonging to this user can be changed
        oldTemplate = getTemplateById(template.getId(), user);
        if (oldTemplate == null) {
            throw new EntityNotFoundException("Template " + template.getTemplateName() + " id=" + template.getId() + " was not found and cannot be saved.");
        }

        oldTemplate.setTemplateName(template.getTemplateName());
        oldTemplate.setTemplateLastUpdated(changeDate);

        //Configure item IDs - if they exist in current report or not
        List<ReportItem> newTemplateItems = new ArrayList<>();
        for (ReportItem reportItem : template.getTemplateItems()) {
            Optional<ReportItem> existingItem = oldTemplate.getTemplateItems().stream()
                    .filter(i -> i.getId().equals(reportItem.getId()))
                    .findAny();
            //If item with this ID does not exist - we will create new record in DB
            if (existingItem.isEmpty()) {
                reportItem.setId(null);
            }

            //Change other ID to null if they did not exist before
            if (reportItem instanceof TextItem textItem) {
                Optional<ReportItem> existingStyle = oldTemplate.getTemplateItems().stream()
                        .filter(i -> ((TextItem) i).getTextStyle().getId().equals(textItem.getTextStyle().getId()))
                        .findAny();
                if (existingStyle.isEmpty()) {
                    textItem.getTextStyle().setId(null);
                }
            } else if (reportItem instanceof TableItem tableItem) {
                handleTableColumnIdsOnUpdate(oldTemplate, tableItem.getTableColumns());
            } else if (reportItem instanceof CapabilityTable capabilityTable) {
                handleTableColumnIdsOnUpdate(oldTemplate, capabilityTable.getTableColumns());
            }

            //Add the correct item
            reportItem.setReport(null);
            newTemplateItems.add(reportItem);
        }
        //Add all new items to list
        oldTemplate.getTemplateItems().clear();
        oldTemplate.getTemplateItems().addAll(newTemplateItems);
        return oldTemplate;
    }


    private void handleTableColumnIdsOnUpdate(Template oldTemplate, List<TableColumn> tableColumns) {
        for(TableColumn tableColumn : tableColumns) {
            Optional<ReportItem> existingTableColumn = oldTemplate.getTemplateItems().stream()
                    .filter(i -> ((TableItem)i).getTableColumns().stream().anyMatch(tc -> tc.getId().equals(tableColumn.getId())))
                    .findAny();
            if (existingTableColumn.isEmpty()) {
                tableColumn.setId(null);
            }
        }
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
