package com.aspicereporting.service;

import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.UnauthorizedAccessException;
import com.aspicereporting.repository.ReportRepository;
import com.aspicereporting.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReportService {
    @Autowired
    ReportRepository reportRepository;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    JasperService jasperService;

    public void generateReport(Long reportId, User user) {
        Report report = reportRepository.findByIdAndReportUser(reportId, user);
        jasperService.generateReport(report);
    }

    //Get all owned or shared sources
    public List<Report> getAllByUserOrShared(User user) {
        return reportRepository.findDistinctByReportUserOrReportGroupsIn(user, user.getUserGroups());
    }

    public Report getReportById(Long id, User user) {
        return reportRepository.findByIdAndReportUser(id, user);
    }

    @Transactional
    public Report saveOrEditReport(Report report, User user) {
        Report newReport;
        Date changeDate = new Date();
        //Edit existing template
        if (report.getId() != null) {
            //Get template if ID is defined - only templates belonging to this user can be changed
            newReport = getReportById(report.getId(), user);
            if (newReport == null) {
                throw new EntityNotFoundException("Report " + report.getReportName() + " id " + report.getId() + " was not found and cannot be saved.");
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
        for (ReportItem item : newReport.getReportItems()) {
            item.setReport(newReport);
        }
        return reportRepository.save(newReport);
    }

    public void deleteReport(Long reportId, User user) {
        Report report = reportRepository.findByIdAndReportUser(reportId, user);
        if (report == null) {
            throw new EntityNotFoundException("Could not find report with id =" + reportId);
        }
        reportRepository.delete(report);
    }

    public void shareWithGroups(Long reportId, List<Long> groupIds, User user) {
        Report report = reportRepository.findByIdAndReportUser(reportId, user);
        if (report == null) {
            throw new EntityNotFoundException("Could not find report with id = " + report.getId());
        }

        //Get all groups for update
        List<UserGroup> reportGroupList = userGroupRepository.findAllByIdIn(groupIds);

        //Get all removed groups
        Set<UserGroup> removedGroups = new HashSet<>(report.getReportGroups());
        removedGroups.removeAll(reportGroupList);

        //Remove removed groups
        for (UserGroup group : removedGroups) {
            report.removeGroup(group);
        }
        //Add new groups
        for (UserGroup group : reportGroupList) {
            report.addGroup(group);
        }

        reportRepository.save(report);
    }

    public Set<UserGroup> getGroupsForReport(Long reportId, User loggedUser) {
        Report report = reportRepository.findFirstById(reportId);
        if (report == null) {
            throw new EntityNotFoundException("Could not find report with id = " + reportId);
        }
        if (report.getReportUser().getId() != loggedUser.getId()) {
            throw new UnauthorizedAccessException("Only the owner of this report can share it.");
        }

        return report.getReportGroups();
    }
}
