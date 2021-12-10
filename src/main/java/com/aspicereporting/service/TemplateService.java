package com.aspicereporting.service;

import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.items.TemplateItem;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TemplateService {

    @Autowired
    TemplateRepository templateRepository;

    public List<Template> getAllTemplatesByUser(User user) {
        return templateRepository.findAllByTemplateUser(user);
    }

    public Template getTemplateById(Long id,User user){
        return templateRepository.findByTemplateUserAndTemplateId(user, id);
    }

    public void saveOrEditTemplate(Template template, User user) {
        Template newTemplate = null;
        Date changeDate = new Date();
        //Edit existing template
        if(template.getTemplateId() != null) {
            //Get template if ID is defined - only templates belonging to this user can be changed
            newTemplate = getTemplateById(template.getTemplateId(), user);
            if(newTemplate == null) {
                throw new EntityNotFoundException("Template " + template.getTemplateName() + " id " + template.getTemplateId() + " was not found and cannot be saved.");
            }

            newTemplate.setTemplateName(template.getTemplateName());
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
        templateRepository.save(newTemplate);
    }
}
