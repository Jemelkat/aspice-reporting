package com.aspicereporting.service;

import com.aspicereporting.entity.*;
import com.aspicereporting.entity.items.*;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.repository.*;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReportService {
    @Autowired
    ReportRepository reportRepository;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    TemplateRepository templateRepository;
    @Autowired
    JasperService jasperService;
    @Autowired
    ItemValidationService itemValidationService;

    public void generateReport(Long reportId, User user) throws JRException {
        Report report = reportRepository.findByIdAndReportUser(reportId, user);
        jasperService.generateReport(report);
    }

    //Get all owned or shared sources
    public List<Report> getAllByUser(User user) {
        return reportRepository.findAllByReportUser(user);
    }

    public Report getReportById(Long id, User user) {
        return reportRepository.findByIdAndReportUser(id, user);
    }

    @Transactional
    public Report saveOrEdit(Report updatedReport, User user) {
        Report oldReport;
        Date changeDate = new Date();
        //Update
        if (updatedReport.getId() != null) {
            oldReport = reportRepository.findByIdAndReportUser(updatedReport.getId(), user);
            if (oldReport == null) {
                throw new EntityNotFoundException("Report " + updatedReport.getReportName() + " id=" + updatedReport.getId() + " was not found and cannot be saved.");
            }
            oldReport.setReportLastUpdated(changeDate);

            //Update report template
            if (updatedReport.getReportTemplate() != null && updatedReport.getReportTemplate().getId() != null) {
                Template template = templateRepository.findByTemplateUserAndId(user, updatedReport.getReportTemplate().getId());
                if (template == null) {
                    throw new EntityNotFoundException("Template id=" + updatedReport.getReportTemplate().getId() + " does not exist or you cannot access this template.");
                }
                oldReport.addTemplate(template);
            } else {
                oldReport.setReportTemplate(null);
            }
        }
        //Create
        else {
            oldReport = updatedReport;
            oldReport.setId(null);
            oldReport.setReportCreated(changeDate);
            oldReport.setReportUser(user);
            if (oldReport.getReportTemplate() != null) {
                Template template = templateRepository.findByTemplateUserAndId(user, oldReport.getReportTemplate().getId());
                if (template == null) {
                    throw new EntityNotFoundException("Template id=" + oldReport.getReportTemplate().getId() + " does not exist or you cannot access this template.");
                }
            }
        }

        //Update name and Template
        oldReport.setReportName(updatedReport.getReportName());

        //Configure item IDs - if they exist use same ID - hibernate will MERGE
        List<ReportItem> newTemplateItems = new ArrayList<>();
        for (ReportItem reportItem : updatedReport.getReportItems()) {
            Optional<ReportItem> existingItem = Optional.empty();
            if (oldReport.getId() != null) {
                existingItem = oldReport.getReportItems().stream()
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
                if (reportItem.getId() == null) {
                    textItem.getTextStyle().setId(null);
                } else {
                    //Use existing textStyle ID
                    textItem.getTextStyle().setId(((TextItem) existingItem.get()).getTextStyle().getId());
                }
                //Bidirectional
                textItem.getTextStyle().setTextItem(textItem);
            }

            //Validate report item if all related sources etc. can be accessed by this user
            itemValidationService.validateItem(reportItem, user);

            reportItem.setReport(oldReport);
            reportItem.setTemplate(null);
            newTemplateItems.add(reportItem);
        }
        oldReport.getReportItems().clear();
        oldReport.getReportItems().addAll(newTemplateItems);

        return reportRepository.save(oldReport);
    }

    public void deleteReport(Long reportId, User user) {
        Report report = reportRepository.findByIdAndReportUser(reportId, user);
        if (report == null) {
            throw new EntityNotFoundException("Could not find report with id =" + reportId);
        }
        reportRepository.delete(report);
    }
}
