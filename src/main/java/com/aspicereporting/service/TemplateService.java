package com.aspicereporting.service;

import com.aspicereporting.entity.*;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.items.TextItem;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.UnauthorizedAccessException;
import com.aspicereporting.repository.TemplateRepository;
import com.aspicereporting.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TemplateService {

    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    UserGroupRepository userGroupRepository;

    //Get all owned or shared sources
    public List<Template> getAllByUserOrShared(User user) {
        return templateRepository.findDistinctByTemplateUserOrTemplateGroupsIn(user, user.getUserGroups());
    }

    public Template getTemplateById(Long id, User user) {
        return templateRepository.findByTemplateUserAndId(user, id);
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
                throw new EntityNotFoundException("Template " + template.getTemplateName() + " id " + template.getId() + " was not found and cannot be saved.");
            }

            newTemplate.setTemplateName(template.getTemplateName());
            newTemplate.setTemplateLastUpdated(changeDate);

            //Configure item IDs - if they exist in current report or not
            List<ReportItem> newTemplateItems = new ArrayList<>();
            for (ReportItem reportItem : template.getTemplateItems()) {
                Optional<ReportItem> existingItem = newTemplate.getTemplateItems().stream()
                        .filter(i -> i.getId().equals(reportItem.getId()))
                        .findAny();
                if (existingItem.isEmpty()) {
                    //If item with this ID does not exist - we will create new record in DB
                    reportItem.setId(null);
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
            }
        }

        //Reconstruct relationship
        for (ReportItem item : newTemplate.getTemplateItems()) {
            item.setTemplate(newTemplate);
            //TODO improve - new text style is created every time
            if (item instanceof TextItem textItem) {
                if (textItem.getTextStyle() != null && textItem.getTextStyle().isFilled()) {
                    textItem.addTextStyle(textItem.getTextStyle());
                } else {
                    textItem.setTextStyle(null);
                }
            }
        }
        return templateRepository.save(newTemplate);
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
