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
    @Autowired
    ItemValidationService itemValidationService;

    public Dashboard saveDashboard(Dashboard updatedDashboard, User user) {
        Dashboard savedDashboard = dashboardRepository.findByDashboardUser(user);
        //CREATE NEW
        if (savedDashboard == null) {
            savedDashboard = updatedDashboard;
            savedDashboard.setId(null);
            savedDashboard.setDashboardUser(user);
            for (ReportItem reportItem : savedDashboard.getDashboardItems()) {
                reportItem.setId(null);
            }
        }

        //Check if dashboard has only valid items
        if (!containsValidItems(savedDashboard)) {
            throw new InvalidDataException("Dashboard accepts only CAPABILITY BAR GRAPH.");
        }

        List<ReportItem> newDashboardItems = new ArrayList<>();
        for (ReportItem reportItem : updatedDashboard.getDashboardItems()) {
            //Configure item IDs - if they exist use same ID - hibernate will MERGE
            if (reportItem.getId() != null) {
                Optional<ReportItem> existingItem = Optional.empty();
                existingItem = savedDashboard.getDashboardItems().stream()
                        .filter(i -> i.getId().equals(reportItem.getId()))
                        .findAny();
                //If item with this ID does not exist - we will create new record in DB
                if (existingItem.isEmpty()) {
                    reportItem.setId(null);
                }
            }

            //Validate report item if all related sources etc. can be accessed by this user
            itemValidationService.validateItem(reportItem, user);
            //Bidirectional relationship
            reportItem.setDashboard(savedDashboard);
            newDashboardItems.add(reportItem);
        }
        savedDashboard.getDashboardItems().clear();
        savedDashboard.getDashboardItems().addAll(newDashboardItems);

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