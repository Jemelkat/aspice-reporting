package com.aspicereporting.service;

import com.aspicereporting.entity.*;
import com.aspicereporting.entity.items.*;
import com.aspicereporting.exception.ConstraintException;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.repository.*;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.IntStream;

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
        jasperService.generateReport(report, user);
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

            List<ReportPage> newReportPages = new ArrayList<>();
            for(ReportPage reportPage : updatedReport.getReportPages()) {
                int existsIndex = IntStream.range(0, oldReport.getReportPages().size())
                        .filter(i -> oldReport.getReportPages().get(i).getId().equals(reportPage.getId()))
                        .findFirst().orElse(-1);
                if(existsIndex == -1 ) {
                    reportPage.setId(null);
                }

                newReportPages.add(reportPage);
            }
            oldReport.getReportPages().clear();
            oldReport.getReportPages().addAll(newReportPages);
        }
        //Create
        else {
            oldReport = updatedReport;
            oldReport.setId(null);
            oldReport.setReportCreated(changeDate);
            oldReport.setReportUser(user);
            for (ReportPage reportPage : oldReport.getReportPages()) {
                if (reportPage.getPageTemplate() != null) {
                    Template template = templateRepository.findByTemplateUserAndId(user, reportPage.getPageTemplate().getId());
                    if (template == null) {
                        throw new EntityNotFoundException("Template name = " + reportPage.getPageTemplate().getTemplateName() + " does not exist or you cannot access this template.");
                    }
                }
            }

            for (ReportPage reportPage : oldReport.getReportPages()) {
                reportPage.setId(null);
                reportPage.setReport(oldReport);
            }
        }

        //Update name and Orientation
        oldReport.setReportName(updatedReport.getReportName());
        for (ReportPage reportPage : oldReport.getReportPages()) {
            //Configure item IDs - if they exist use same ID - hibernate will MERGE
            List<ReportItem> reportItems = new ArrayList<>();
            for (ReportItem reportItem : reportPage.getReportItems()) {
                Optional<ReportItem> existingItem = Optional.empty();
                if (reportPage.getId() != null) {
                    existingItem = reportPage.getReportItems().stream()
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
                itemValidationService.validateItem(reportItem, true, user);

                reportItem.setReportPage(reportPage);
                reportItem.setTemplate(null);
                reportItem.setDashboard(null);
                reportItems.add(reportItem);

            }
            reportPage.setReport(oldReport);
            reportPage.getReportItems().clear();
            reportPage.getReportItems().addAll(reportItems);
        }
        //oldReport.setReportPages(reportPages);
        try {
            return reportRepository.save(oldReport);
        } catch (DataIntegrityViolationException e) {
            if (e.getMostSpecificCause().getClass().getName().equals("org.postgresql.util.PSQLException") && ((SQLException) e.getMostSpecificCause()).getSQLState().equals("23505"))
                throw new ConstraintException("There is already report with this name.", e.getMostSpecificCause());
            throw new InvalidDataException("Error saving report", e);
        }

    }

    public void deleteReport(Long reportId, User user) {
        Report report = reportRepository.findByIdAndReportUser(reportId, user);
        if (report == null) {
            throw new EntityNotFoundException("Could not find report with id =" + reportId);
        }
        reportRepository.delete(report);
    }
}
