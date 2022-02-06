package com.aspicereporting.service;

import com.aspicereporting.entity.*;
import com.aspicereporting.entity.items.*;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.exception.UnauthorizedAccessException;
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
            } else if (reportItem instanceof TableItem tableItem) {
                //Validate - columns are defined
                if (tableItem.getTableColumns().isEmpty()) {
                    throw new InvalidDataException("Simple table has no columns defined.");
                }
                //Validate - source is defined
                if (tableItem.getSource().getId() == null) {
                    throw new InvalidDataException("Simple table has no source defined.");
                }

                Long sourceId = tableItem.getSource().getId();
                //Validate - user can use this source id
                Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
                if (source == null) {
                    throw new EntityNotFoundException("You dont have any source with id=" + sourceId);
                }

                //Table columns are reinserted every time - not updated
                tableItem.getTableColumns().forEach(tableColumn -> {
                    //Validate if column exists in source
                    Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(tableColumn.getSourceColumn().getId())).findFirst();
                    if (columnExists.isEmpty()) {
                        throw new EntityNotFoundException("Invalid source column id=" + tableColumn.getSourceColumn().getId() + " for source id=" + sourceId);
                    }
                    tableColumn.setId(null);
                });
                source.addSimpleTable(tableItem);
            } else if (reportItem instanceof CapabilityTable capTable) {
                //Validate - columns are defined
                if (capTable.getProcessColumn() == null || capTable.getProcessColumn().getSourceColumn() == null) {
                    throw new InvalidDataException("capability table has no process column defined.");
                }
                //Validate - source is defined
                if (capTable.getSource().getId() == null) {
                    throw new InvalidDataException("Simple table has no source defined.");
                }
                Long sourceId = capTable.getSource().getId();
                //Validate - user can use this source id
                Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
                if (source == null) {
                    throw new EntityNotFoundException("You dont have any source with id=" + sourceId);
                }

                //PROCESS VALIDATE
                Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capTable.getProcessColumn().getSourceColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capTable.getProcessColumn().getSourceColumn().getId() + " for source id=" + sourceId);
                }
                //LEVEL VALIDATE
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capTable.getLevelColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capTable.getLevelColumn().getId() + " for source id=" + sourceId);
                }
                //ENGINEERING VALIDATE
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capTable.getEngineeringColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capTable.getEngineeringColumn().getId() + " for source id=" + sourceId);
                }
                //SCORE VALIDATE
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capTable.getScoreColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capTable.getScoreColumn().getId() + " for source id=" + sourceId);
                }

                capTable.getProcessColumn().setId(null);
            } else if (reportItem instanceof CapabilityBarGraph capabilityBarGraph) {
                //Validate if user filled all required fields
                capabilityBarGraph.validate();

                Long sourceId = capabilityBarGraph.getSource().getId();
                //Validate - user can use this source id
                Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
                if (source == null) {
                    throw new EntityNotFoundException("You dont have access to this source id = " + sourceId);
                }
                //PROCESS VALIDATE
                Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityBarGraph.getProcessColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityBarGraph.getProcessColumn().getId() + " for source id=" + sourceId);
                }
                //LEVEL VALIDATE
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityBarGraph.getLevelColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityBarGraph.getLevelColumn().getId() + " for source id=" + sourceId);
                }
                //ATTRIBUTE VALIDATE
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityBarGraph.getAttributeColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityBarGraph.getAttributeColumn().getId() + " for source id=" + sourceId);
                }
                //SCORE VALIDATE
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityBarGraph.getScoreColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityBarGraph.getScoreColumn().getId() + " for source id=" + sourceId);
                }

                source.addCapabilityGraph(capabilityBarGraph);
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
}
