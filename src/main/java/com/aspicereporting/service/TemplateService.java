package com.aspicereporting.service;

import com.aspicereporting.entity.*;
import com.aspicereporting.entity.items.TemplateItem;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.UnauthorizedAccessException;
import com.aspicereporting.repository.TemplateRepository;
import com.aspicereporting.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TemplateService {

    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    UserGroupRepository userGroupRepository;

    public List<Template> getAllTemplatesByUser(User user) {
        return templateRepository.findAllByTemplateUser(user);
    }

    public Template getById(Long id, User user){
        return templateRepository.findByTemplateUserAndTemplateId(user, id);
    }

    @Transactional
    public Template saveOrEditTemplate(Template template, User user) {
        Template newTemplate;
        Date changeDate = new Date();
        //Edit existing template
        if(template.getTemplateId() != null) {
            //Get template if ID is defined - only templates belonging to this user can be changed
            newTemplate = getById(template.getTemplateId(), user);
            if(newTemplate == null) {
                throw new EntityNotFoundException("Template " + template.getTemplateName() + " id " + template.getTemplateId() + " was not found and cannot be saved.");
            }

            newTemplate.setTemplateName(template.getTemplateName());
            newTemplate.setTemplateLastUpdated(changeDate);

            //Cant change the collection add new one instead
            newTemplate.getTemplateItems().clear();
            newTemplate.getTemplateItems().addAll(template.getTemplateItems());
        }
        //Create new template
        else {
            newTemplate = template;
            newTemplate.setTemplateCreated(changeDate);
            newTemplate.setTemplateUser(user);
        }

        //Reconstruct relationship
        for(TemplateItem item: newTemplate.getTemplateItems()) {
            item.setTemplate(newTemplate);
        }
        return templateRepository.save(newTemplate);
    }

    public void shareWithGroups(Long templateId, List<Long> groupIds, User user) {
        Template template = templateRepository.findByTemplateUserAndTemplateId(user, templateId);
        if(template == null) {
            throw new EntityNotFoundException("Could not find template with id = " + template.getTemplateId());
        }

        //Get all groups for update
        List<UserGroup> templateGroupList = userGroupRepository.findAllByIdIn(groupIds);

        //Get all removed groups
        Set<UserGroup> removedGroups = new HashSet<>(template.getTemplateGroups());
        removedGroups.removeAll(templateGroupList);

        //Remove removed groups
        for(UserGroup group : removedGroups) {
            template.removeGroup(group);
        }
        //Add new groups
        for(UserGroup group : templateGroupList) {
            template.addGroup(group);
        }

        templateRepository.save(template);
    }

    public Set<UserGroup> getGroupsForTemplate(Long templateId, User loggedUser) {
        Template template = templateRepository.findByTemplateId(templateId);
        if(template == null) {
            throw new EntityNotFoundException("Could not find source with id = " + templateId);
        }
        if(template.getTemplateUser().getId() != loggedUser.getId()) {
            throw new UnauthorizedAccessException("Only the owner of this source can share it.");
        }

        return template.getTemplateGroups();
    }

    public void delete(Long templateId, User user) {
        Template template = templateRepository.findByTemplateUserAndTemplateId(user, templateId);
        if(template==null) {
            throw new EntityNotFoundException("Could not find template with id =" + templateId );
        }

        //Remove foreign key in reports
        for(Report r : template.getReports()) {
            r.setReportTemplate(null);
        }

        templateRepository.delete(template);
    }
}
