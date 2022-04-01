package com.aspicereporting.service;

import com.aspicereporting.entity.*;
import com.aspicereporting.entity.items.*;
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
import java.util.*;

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
        for (ReportItem templateItem : updatedTemplate.getTemplateItems()) {
            Optional<ReportItem> existingItem = Optional.empty();
            if (oldTemplate.getId() != null) {
                existingItem = oldTemplate.getTemplateItems().stream()
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

            templateItem.setTemplate(oldTemplate);
            templateItem.setReport(null);
            newTemplateItems.add(templateItem);
        }
        oldTemplate.getTemplateItems().clear();
        oldTemplate.getTemplateItems().addAll(newTemplateItems);
        try {
            return templateRepository.save(oldTemplate);
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
        for (Report r : template.getReports()) {
            r.setReportTemplate(null);
        }

        templateRepository.delete(template);
    }
}
