package com.aspicereporting.service;

import com.aspicereporting.entity.Dashboard;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.items.LevelBarGraph;
import com.aspicereporting.entity.items.LevelPieGraph;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.jasper.service.LevelBarGraphService;
import com.aspicereporting.jasper.service.LevelPieGraphService;
import com.aspicereporting.repository.DashboardRepository;
import com.aspicereporting.repository.SourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardService {
    @Autowired
    DashboardRepository dashboardRepository;
    @Autowired
    SourceRepository sourceRepository;
    @Autowired
    ItemValidationService itemValidationService;
    @Autowired
    LevelBarGraphService levelBarGraphService;
    @Autowired
    LevelPieGraphService levelPieGraphService;

    public Dashboard saveDashboard(Dashboard dashboard, User user) {
        Dashboard newDashboard = dashboardRepository.findByDashboardUser(user);
        //CREATE NEW
        if (newDashboard == null) {
            newDashboard = dashboard;
            newDashboard.setId(null);
            newDashboard.setDashboardUser(user);
            for (ReportItem reportItem : newDashboard.getDashboardItems()) {
                reportItem.setId(null);
            }
        }

        //Check if dashboard has only valid items
        if (!containsValidItems(newDashboard)) {
            throw new InvalidDataException("Dashboard accepts only bar or pie graph.");
        }

        List<ReportItem> newDashboardItems = new ArrayList<>();
        for (ReportItem reportItem : dashboard.getDashboardItems()) {
            //Configure item IDs - if they exist use same ID - hibernate will MERGE
            if (reportItem.getId() != null) {
                Optional<ReportItem> existingItem = Optional.empty();
                existingItem = newDashboard.getDashboardItems().stream()
                        .filter(i -> i.getId().equals(reportItem.getId()))
                        .findAny();
                //If item with this ID does not exist - we will create new record in DB
                if (existingItem.isEmpty()) {
                    reportItem.setId(null);
                }
            }

            //Validate report item if all related sources etc. can be accessed by this user
            itemValidationService.validateItem(reportItem, true, user);
            //Bidirectional relationship
            reportItem.setDashboard(newDashboard);
            reportItem.setReportPage(null);
            reportItem.setTemplate(null);
            newDashboardItems.add(reportItem);
        }
        newDashboard.getDashboardItems().clear();
        newDashboard.getDashboardItems().addAll(newDashboardItems);

        return dashboardRepository.save(newDashboard);
    }

    public Dashboard getDashboardByUser(User user) {
        return dashboardRepository.findByDashboardUser(user);
    }

    public List<Map<String, String>> getDashboardItemData(Long itemId, User user) {
        Dashboard dashboard = dashboardRepository.findByDashboardUser(user);
        if (dashboard == null) {
            throw new EntityNotFoundException("You don't have any saved dashboard.");
        }
        //Check if dashboard has only valid items
        if (!containsValidItems(dashboard)) {
            throw new InvalidDataException("Dashboard accepts only CAPABILITY BAR GRAPH or PIE GRAPH.");
        }

        Optional<ReportItem> existingItem = Optional.empty();
        existingItem = dashboard.getDashboardItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findAny();
        if (existingItem.isEmpty()) {
            throw new EntityNotFoundException("You don't have this dashboard item saved.");
        }


        ReportItem reportItem = existingItem.get();
        //Validate report item if all related sources etc. can be accessed by this user - required for generation
        itemValidationService.validateItemWithValid(reportItem,  user);


        List<Map<String, String>> result = new ArrayList<>();
        if (reportItem instanceof LevelPieGraph levelPieGraph) {
            LinkedHashMap<String, Integer> map = levelPieGraphService.getData(levelPieGraph);
            for(var level : map.keySet()) {
                result.add(Map.of("level", level, "count", map.get(level).toString()));
            }
        } else if (reportItem instanceof LevelBarGraph levelBarGraph) {
            LinkedHashMap<String, LinkedHashMap<String, Integer>> map = levelBarGraphService.getData(levelBarGraph);
            Set<String> assessorSet = new LinkedHashSet<>();
            for(var process : map.keySet()){
                for(var assessor : map.get(process).keySet()) {
                    assessorSet.add(assessor);
                }
            }
                for(var process : map.keySet()){
                for(var assessor : assessorSet) {
                    result.add(Map.of("process", process, "assessor", assessor, "level", map.get(process).containsKey(assessor) ? map.get(process).get(assessor).toString() : "0"));
                    assessorSet.add(assessor);
                }
            }
        } else {
            throw new InvalidDataException("Invalid item type provided :" + reportItem.getType().toString());
        }
        return result;
    }

    private boolean containsValidItems(Dashboard dashboard) {
        for (ReportItem item : dashboard.getDashboardItems()) {
            switch (item.getType()) {
                case LEVEL_PIE_GRAPH:
                case LEVEL_BAR_GRAPH:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }
}
