package com.aspicereporting.service;

import com.aspicereporting.entity.*;
import com.aspicereporting.entity.items.TableColumn;
import com.aspicereporting.entity.items.TableItem;
import com.aspicereporting.entity.items.TextItem;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.exception.UnauthorizedAccessException;
import com.aspicereporting.repository.*;
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
    TemplateRepository templateRepository;
    @Autowired
    SourceRepository sourceRepository;
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

    public Report save(Report updatedReport, User user) {
        Report oldReport;
        Date changeDate = new Date();
        //Update
        if (updatedReport.getId() != null) {
            oldReport = getReportById(updatedReport.getId(), user);
            if (oldReport == null) {
                throw new EntityNotFoundException("Report " + updatedReport.getReportName() + " id=" + updatedReport.getId() + " was not found and cannot be saved.");
            }
            oldReport.setReportLastUpdated(changeDate);

            //Update report template
            if (updatedReport.getReportTemplate() != null && updatedReport.getReportTemplate().getId() != null) {
                Template template = templateRepository.findByTemplateUserAndIdOrTemplateGroupsIn(user, updatedReport.getReportTemplate().getId(), user.getUserGroups());
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
                Template template = templateRepository.findByTemplateUserAndIdOrTemplateGroupsIn(user, oldReport.getReportTemplate().getId(), user.getUserGroups());
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
                if (reportItem.getId() == null || !(existingItem.get() instanceof TextItem)) {
                    textItem.getTextStyle().setId(null);
                } else {
                    //Use existing textStyle ID
                    textItem.getTextStyle().setId(((TextItem) existingItem.get()).getTextStyle().getId());
                }
                //Bidirectional
                textItem.getTextStyle().setTextItem(textItem);
            } else if (reportItem instanceof TableItem tableItem) {
                //Validate - columns are defined
                if (tableItem.getTableColumns().isEmpty()) {
                    throw new InvalidDataException("Simple table has no columns defined.");
                }
                //Validate - source is defined
                if (tableItem.getTableColumns().get(0).getSource().getId() == null) {
                    throw new InvalidDataException("Simple table has no source defined.");
                }
                //Validate - all source columns need to be the same
                Long firstId = tableItem.getTableColumns().get(0).getSource().getId();
                List<TableColumn> sameSourceColumns = tableItem.getTableColumns().stream().takeWhile((i -> firstId == i.getSource().getId())).collect(Collectors.toList());
                if (sameSourceColumns.size() != tableItem.getTableColumns().size()) {
                    throw new InvalidDataException("Simple table accepts only columns from one data source!");
                }
                //Validate - user can use this source id
                Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(firstId, user, user.getUserGroups());
                if (source == null) {
                    throw new EntityNotFoundException("You dont have any source with id=" + firstId);
                }

                //Table columns are reinserted every time - not updated
                tableItem.getTableColumns().forEach(tableColumn -> {
                    //Validate if column exists in source
                    Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> c.getId() == tableColumn.getSourceColumn().getId()).findFirst();
                    if (columnExists.isEmpty()) {
                        throw new EntityNotFoundException("Invalid source column id=" + tableColumn.getSourceColumn().getId() + " for source id=" + tableColumn.getSource().getId());
                    }
                    tableColumn.setId(null);
                    //Bidirectional
                    tableColumn.setSimpleTable(tableItem);
                });
            } else {
                throw new InvalidDataException("Report contains unknown item type: " + reportItem.getType());
            }
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
