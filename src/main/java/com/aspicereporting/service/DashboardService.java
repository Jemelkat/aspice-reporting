package com.aspicereporting.service;

import com.aspicereporting.entity.Dashboard;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.items.*;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.jasper.service.CapabilityBarGraphService;
import com.aspicereporting.jasper.service.LevelPieGraphService;
import com.aspicereporting.jasper.service.SourceLevelBarGraphService;
import com.aspicereporting.repository.DashboardRepository;
import com.aspicereporting.repository.SourceRepository;
import com.aspicereporting.utils.NaturalOrderComparator;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    @Autowired
    DashboardRepository dashboardRepository;
    @Autowired
    SourceRepository sourceRepository;
    @Autowired
    ItemValidationService itemValidationService;
    @Autowired
    CapabilityBarGraphService capabilityBarGraphService;
    @Autowired
    SourceLevelBarGraphService sourceLevelBarGraphService;
    @Autowired
    LevelPieGraphService levelPieGraphService;

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
            throw new InvalidDataException("Dashboard accepts only bar or pie graph.");
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
            itemValidationService.validateItem(reportItem, true, user);
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
        itemValidationService.validateItemWithValid(reportItem, false, user);


        List<Map<String, String>> result = new ArrayList<>();
        if (reportItem instanceof CapabilityBarGraph capabilityBarGraph) {
            /*Returns list of map items
             * [{process: "name", assessor: "name", level: 0}, ...]
             * */
            LinkedHashMap<String, Map<String, Integer>> map = capabilityBarGraphService.getData(capabilityBarGraph);
            for(var process : map.keySet()){
                for(var assessor : map.get(process).keySet()) {
                    result.add(Map.of("process", process, "assessor", assessor, "level", map.get(process).get(assessor).toString()));
                }
            }
        } else if (reportItem instanceof LevelPieGraph levelPieGraph) {
            LinkedHashMap<String, Integer> map = levelPieGraphService.getData(levelPieGraph);
            for(var level : map.keySet()) {
                result.add(Map.of("level", level, "count", map.get(level).toString()));
            }
        } else if (reportItem instanceof SourceLevelBarGraph sourceLevelBarGraph) {
            LinkedHashMap<String, Map<String, Integer>> map = sourceLevelBarGraphService.getData(sourceLevelBarGraph);
            for(var level : map.keySet()) {
                //result.add(Map.of("level", level, "count", map.get(level).toString()));
                System.out.println(level);
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
                case CAPABILITY_BAR_GRAPH:
                case SOURCE_LEVEL_BAR_GRAPH:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }
}
