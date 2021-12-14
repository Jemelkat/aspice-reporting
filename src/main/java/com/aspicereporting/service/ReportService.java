package com.aspicereporting.service;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.items.TemplateItem;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ReportService {
    @Autowired
    ReportRepository reportRepository;

    public List<Report> getAllReportsByUser(User user) {
        return reportRepository.findAllByReportUser(user);
    }

    public Report getReportById(Long id, User user) {
        return reportRepository.findByReportIdAndReportUser(id, user);
    }

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
}
