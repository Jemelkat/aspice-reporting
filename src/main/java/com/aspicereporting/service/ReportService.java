package com.aspicereporting.service;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.ReportPage;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.items.TextItem;
import com.aspicereporting.exception.ConstraintException;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.repository.ReportRepository;
import com.aspicereporting.repository.TemplateRepository;
import com.aspicereporting.repository.UserGroupRepository;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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

    public ByteArrayOutputStream generateReportById(Long reportId, User user) throws JRException {
        Report report = reportRepository.findByIdAndReportUser(reportId, user);
        if(report == null) {
            throw new EntityNotFoundException("Report id=" + reportId + " not found or inaccessible.");
        }
        return jasperService.generateReport(report, user);
    }

    //Get all owned or shared sources
    public List<Report> getAllByUser(User user) {
        return reportRepository.findAllByReportUser(user);
    }

    public Report getReportById(Long id, User user) {
        return reportRepository.findByIdAndReportUser(id, user);
    }

    public Report saveOrEdit(Report report, User user) {
        Report newReport;
        Date changeDate = new Date();
        //Update
        if (report.getId() != null) {
            newReport = reportRepository.findByIdAndReportUser(report.getId(), user);
            if (newReport == null) {
                throw new EntityNotFoundException("Report " + report.getReportName() + " id=" + report.getId() + " was not found and cannot be saved.");
            }
            newReport.setReportLastUpdated(changeDate);

            List<ReportPage> newReportPages = new ArrayList<>();
            //Keeps report pages with same ID - hibernate will update instead of insert
            for (ReportPage reportPage : report.getReportPages()) {
                int existsIndex = IntStream.range(0, newReport.getReportPages().size())
                        .filter(i -> newReport.getReportPages().get(i).getId().equals(reportPage.getId()))
                        .findFirst().orElse(-1);
                if (existsIndex == -1) {
                    reportPage.setId(null);
                }
                for(ReportItem reportItem : reportPage.getReportItems()) {
                    reportItem.setId(null);
                }
                newReportPages.add(reportPage);
            }
            //Set new pages to report
            newReport.getReportPages().clear();
            newReport.getReportPages().addAll(newReportPages);
        }
        //Create
        else {
            newReport = report;
            newReport.setId(null);
            newReport.setReportCreated(changeDate);
            newReport.setReportUser(user);
            for (ReportPage reportPage : newReport.getReportPages()) {
                if (reportPage.getPageTemplate() != null) {
                    Template template = templateRepository.findByTemplateUserAndId(user, reportPage.getPageTemplate().getId());
                    if (template == null) {
                        throw new EntityNotFoundException("Template name = " + reportPage.getPageTemplate().getTemplateName() + " does not exist or you cannot access this template.");
                    }
                }
            }

            for (ReportPage reportPage : newReport.getReportPages()) {
                reportPage.setId(null);
                reportPage.setReport(newReport);
            }
        }

        //Update name and Orientation
        newReport.setReportName(report.getReportName());
        for (ReportPage reportPage : newReport.getReportPages()) {
            //Configure item IDs - if they exist use same ID - hibernate will MERGE
            List<ReportItem> reportItems = new ArrayList<>();
            for (ReportItem reportItem : reportPage.getReportItems()) {
                Optional<ReportItem> existingItem = Optional.empty();
                if (reportItem.getId() != null) {
                    if (reportPage.getId() != null) {
                        existingItem = reportPage.getReportItems().stream()
                                .filter(i -> i.getId().equals(reportItem.getId()))
                                .findAny();
                    }
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
            reportPage.setReport(newReport);
            reportPage.getReportItems().clear();
            reportPage.getReportItems().addAll(reportItems);
        }
        try {
            return reportRepository.save(newReport);
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
