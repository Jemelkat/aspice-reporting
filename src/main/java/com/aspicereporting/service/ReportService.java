package com.aspicereporting.service;

import com.aspicereporting.entity.items.TextItem;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.UnauthorizedAccessException;
import com.aspicereporting.repository.ReportItemsRepository;
import com.aspicereporting.repository.ReportRepository;
import com.aspicereporting.repository.UserGroupRepository;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
    @Autowired
    ReportRepository reportRepository;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    ReportItemsRepository reportItemsRepository;
    @Autowired
    JasperService jasperService;

    public void generateReport(Long reportId, User user) throws JRException {
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
        //Edit existing report
        if (report.getId() != null) {
            //Get template if ID is defined - only templates belonging to this user can be changed
            newReport = getReportById(report.getId(), user);
            if (newReport == null) {
                throw new EntityNotFoundException("Report " + report.getReportName() + " id " + report.getId() + " was not found and cannot be saved.");
            }

            newReport.setReportName(report.getReportName());
            newReport.setReportLastUpdated(changeDate);
            newReport.setReportTemplate(report.getReportTemplate());

            //Configure item IDs - if they exist in current report or not
            List<ReportItem> newReportItems = new ArrayList<>();
            for (ReportItem reportItem : report.getReportItems()) {
                Optional<ReportItem> existingItem = newReport.getReportItems().stream()
                        .filter(p -> p.getId().equals(reportItem.getId()))
                        .findAny();
                if (existingItem.isEmpty()) {
                    //If item with this ID does not exist - we will create new record in DB
                    reportItem.setId(null);
                }

                //Add the correct item to temporary list
                reportItem.setTemplate(null);
                newReportItems.add(reportItem);
            }
            //Add all new items to list
            newReport.getReportItems().clear();
            newReport.getReportItems().addAll(newReportItems);
        }
        //Create new report
        else {
            newReport = report;
            newReport.setReportCreated(changeDate);
            newReport.setReportUser(user);
            //Remove ids from items and text style - Will create new items in DB
            for (ReportItem item : newReport.getReportItems()) {
                item.setId(null);
                item.setTemplate(null);
                if (item instanceof TextItem textItem && textItem.getTextStyle() != null) {
                    textItem.getTextStyle().setId(null);
                }
            }
        }


        //Reconstruct relationship
        for (ReportItem item : newReport.getReportItems()) {
            item.setReport(newReport);
            //Add text style to TextItems
            //TODO improve - new text style is created every time
            if (item instanceof TextItem textItem) {
                if (textItem.getTextStyle() != null && textItem.getTextStyle().isFilled()) {
                    textItem.addTextStyle(textItem.getTextStyle());
                } else {
                    textItem.setTextStyle(null);
                }
            }
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
