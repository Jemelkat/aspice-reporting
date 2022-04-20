package com.aspicereporting.service;

import com.aspicereporting.entity.ReportPage;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.items.TextItem;
import com.aspicereporting.exception.ConstraintException;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.repository.SourceRepository;
import com.aspicereporting.repository.TemplateRepository;
import com.aspicereporting.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TemplateService {

    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    SourceRepository sourceRepository;

    @Autowired
    UserGroupRepository userGroupRepository;

    @Autowired
    ItemValidationService itemValidationService;

    public List<Template> getAllByUser(User user) {
        return templateRepository.findAllByTemplateUser(user);
    }

    public Template getTemplateById(Long id, User user) {
        return templateRepository.findByTemplateUserAndId(user, id);
    }

    public Template saveOrEditTemplate(Template template, User user) {
        Template newTemplate;
        Date changeDate = new Date();
        //Update
        if (template.getId() != null) {
            newTemplate = getTemplateById(template.getId(), user);
            if (newTemplate == null) {
                throw new EntityNotFoundException("Template " + template.getTemplateName() + " id=" + template.getId() + " was not found and cannot be saved.");
            }
            newTemplate.setTemplateLastUpdated(changeDate);
        }
        //Create
        else {
            newTemplate = template;
            newTemplate.setId(null);
            newTemplate.setTemplateCreated(changeDate);
            newTemplate.setTemplateUser(user);
        }

        //Update name and orientation
        newTemplate.setTemplateName(template.getTemplateName());
        newTemplate.setOrientation(template.getOrientation());

        //Configure item IDs - if they exist use same ID - hibernate will MERGE
        List<ReportItem> newTemplateItems = new ArrayList<>();
        for (ReportItem templateItem : template.getTemplateItems()) {
            Optional<ReportItem> existingItem = Optional.empty();
            if (newTemplate.getId() != null) {
                existingItem = newTemplate.getTemplateItems().stream()
                        .filter(i -> i.getId().equals(templateItem.getId()))
                        .findAny();
            }
            //If item with this ID does not exist - we will create new record in DB
            if (existingItem.isEmpty()) {
                templateItem.setId(null);
            }

            //Configure and reconstruct relationship items IDs
            if (templateItem instanceof TextItem textItem) {
                //If this item ID was not found or its instance is not TextItem
                if (templateItem.getId() == null || !(existingItem.get() instanceof TextItem)) {
                    textItem.getTextStyle().setId(null);
                } else {
                    //Use existing textStyle ID
                    textItem.getTextStyle().setId(((TextItem) existingItem.get()).getTextStyle().getId());
                }
                //Bidirectional
                textItem.getTextStyle().setTextItem(textItem);
            }

            //Validate template item if all related sources etc. can be accessed by this user
            itemValidationService.validateItem(templateItem, true, user);

            templateItem.setTemplate(newTemplate);
            templateItem.setReportPage(null);
            templateItem.setDashboard(null);
            newTemplateItems.add(templateItem);
        }
        newTemplate.getTemplateItems().clear();
        newTemplate.getTemplateItems().addAll(newTemplateItems);
        try {
            return templateRepository.save(newTemplate);
        } catch (
                DataIntegrityViolationException e) {
            if (e.getMostSpecificCause().getClass().getName().equals("org.postgresql.util.PSQLException") && ((SQLException) e.getMostSpecificCause()).getSQLState().equals("23505"))
                throw new ConstraintException("There is already template with this name.", e.getMostSpecificCause());
            throw new InvalidDataException("Error saving report", e);
        }
    }

    public void deleteTemplate(Long templateId, User user) {
        Template template = templateRepository.findByTemplateUserAndId(user, templateId);
        if (template == null) {
            throw new EntityNotFoundException("Could not find template with id =" + templateId);
        }

        //Remove foreign key in reports
        for (ReportPage r : template.getReportPages()) {
            r.setPageTemplate(null);
        }

        templateRepository.delete(template);
    }
}
