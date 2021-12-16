package com.aspicereporting.service;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.items.TemplateItem;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.repository.ReportRepository;
import com.aspicereporting.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.AccessType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ReportService {
    @Autowired
    ReportRepository reportRepository;
    @Autowired
    UserGroupRepository userGroupRepository;

    public List<Report> getAllReportsForUser(User user) {
        return reportRepository.findAllByReportUserOrReportGroup(user, user.getUserGroup());
    }

    public Report getReportById(Long id, User user) {
        return reportRepository.findByReportIdAndReportUser(id, user);
    }

    @Transactional
    public void saveOrEditReport(Report report, User user) {
        Report newReport;
        Date changeDate = new Date();
        //Edit existing template
        if(report.getReportId() != null) {
            //Get template if ID is defined - only templates belonging to this user can be changed
            newReport = getReportById(report.getReportId(), user);
            if(newReport == null) {
                throw new EntityNotFoundException("Report " + report.getReportName() + " id " + report.getReportId() + " was not found and cannot be saved.");
            }

            newReport.setReportName(report.getReportName());
            newReport.setReportLastUpdated(changeDate);
            newReport.setReportTemplate(report.getReportTemplate());

            //Cant change the collection add new one instead
            newReport.getReportItems().clear();
            newReport.getReportItems().addAll(report.getReportItems());
        }
        //Create new template
        else {
            newReport = report;
            newReport.setReportCreated(changeDate);
            newReport.setReportUser(user);
        }

        //Reconstruct relationship
        for(ReportItem item: newReport.getReportItems()) {
            item.setReport(newReport);
        }
        reportRepository.save(newReport);
    }

    public void deleteReport(Long reportId, User user) {
        Report report = reportRepository.findByReportIdAndReportUser(reportId, user);
        if(report==null) {
            throw new EntityNotFoundException("Could not find report with id =" + reportId );
        }
        reportRepository.delete(report);
    }

    public void shareReport(Long reportId, User user) {
        UserGroup userGroup = userGroupRepository.findByUsersContains(user);
        if(userGroup==null) {
            throw new EntityNotFoundException("You are not in any group.");
        }

        Report report = reportRepository.findByReportIdAndReportUser(reportId, user);
        if(report==null) {
            throw new EntityNotFoundException("Could not find report with id=" + reportId);
        }

        report.setReportGroup(userGroup);
        report.setReportLastUpdated(new Date());
        reportRepository.save(report);
    }
}
