package com.aspicereporting.service;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.items.TemplateItem;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.repository.TemplateRepository;
import com.aspicereporting.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TemplateService {

    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    UserGroupRepository userGroupRepository;

    public List<Template> getAllTemplatesByUser(User user) {
        return templateRepository.findAllByTemplateUser(user);
    }

    public Template getTemplateById(Long id,User user){
        return templateRepository.findByTemplateUserAndTemplateId(user, id);
    }

    @Transactional
    public Template saveOrEditTemplate(Template template, User user) {
        Template newTemplate;
        Date changeDate = new Date();
        //Edit existing template
        if(template.getTemplateId() != null) {
            //Get template if ID is defined - only templates belonging to this user can be changed
            newTemplate = getTemplateById(template.getTemplateId(), user);
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

    public void shareTemplate(Long templateId, User user) {
        UserGroup userGroup = userGroupRepository.findByUsersContains(user);
        if(userGroup==null) {
            throw new EntityNotFoundException("You are not in any group.");
        }

        Template template = templateRepository.findByTemplateUserAndTemplateId(user,templateId);
        if(template==null) {
            throw new EntityNotFoundException("Could not find template with id=" + templateId);
        }

        template.setTemplateGroup(userGroup);
        template.setTemplateLastUpdated(new Date());
        templateRepository.save(template);
    }

    public void deleteTemplate(Long templateId, User user) {
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
