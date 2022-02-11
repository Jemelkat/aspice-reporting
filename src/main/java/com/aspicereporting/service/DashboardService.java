package com.aspicereporting.service;

import com.aspicereporting.entity.Dashboard;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.items.CapabilityBarGraph;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.items.TableItem;
import com.aspicereporting.entity.items.TextItem;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.repository.DashboardRepository;
import com.aspicereporting.repository.SourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {
    @Autowired
    DashboardRepository dashboardRepository;
    @Autowired
    SourceRepository sourceRepository;

    public Dashboard saveDashboard(Dashboard updatedDashboard, User user) {
        Dashboard savedDashboard = dashboardRepository.findByDashboardUser(user);
        //CREATE
        if (savedDashboard == null) {
            savedDashboard = updatedDashboard;
            if (!containsValidItems(savedDashboard)) {
                throw new InvalidDataException("Dashboard accepts only CAPABILITY BAR GRAPH.");
            }

            savedDashboard.setId(null);
            savedDashboard.setDashboardUser(user);
            for (ReportItem reportItem : savedDashboard.getDashboardItems()) {
                reportItem.setId(null);
            }
            //UPDATE
        } else {
            if (!containsValidItems(savedDashboard)) {
                throw new InvalidDataException("Dashboard accepts only CAPABILITY BAR GRAPH.");
            }
            //Configure item IDs - if they exist use same ID - hibernate will MERGE
            List<ReportItem> newDashboardItems = new ArrayList<>();
            for (ReportItem reportItem : updatedDashboard.getDashboardItems()) {
                Optional<ReportItem> existingItem = Optional.empty();
                existingItem = savedDashboard.getDashboardItems().stream()
                        .filter(i -> i.getId().equals(reportItem.getId()))
                        .findAny();
                //If item with this ID does not exist - we will create new record in DB
                if (existingItem.isEmpty()) {
                    reportItem.setId(null);
                }

                if (reportItem instanceof CapabilityBarGraph capabilityBarGraph) {
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
                }

                reportItem.setDashboard(savedDashboard);
                newDashboardItems.add(reportItem);
            }
            savedDashboard.getDashboardItems().clear();
            savedDashboard.getDashboardItems().addAll(newDashboardItems);
        }

        return dashboardRepository.save(savedDashboard);
    }

    public Dashboard getDashboardByUser(User user) {
        return dashboardRepository.findByDashboardUser(user);
    }

    private boolean containsValidItems(Dashboard dashboard) {
        for (ReportItem item : dashboard.getDashboardItems()) {
            switch (item.getType()) {
                case CAPABILITY_BAR_GRAPH:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }
}
