package com.aspicereporting.service;

import com.aspicereporting.entity.Dashboard;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.enums.ItemType;
import com.aspicereporting.entity.items.CapabilityTable;
import com.aspicereporting.entity.items.LevelBarGraph;
import com.aspicereporting.entity.items.LevelPieGraph;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.jasper.service.LevelBarGraphService;
import com.aspicereporting.jasper.service.LevelPieGraphService;
import com.aspicereporting.repository.DashboardRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DashboardTests {

    @Mock
    DashboardRepository dashboardRepository;
    @Mock
    ItemValidationService itemValidationService;
    @Mock
    LevelBarGraphService levelBarGraphService;
    @Mock
    LevelPieGraphService levelPieGraphService;
    @InjectMocks
    DashboardService dashboardService;



    @DisplayName("Missing dashboard test")
    @Test
    public void missingDashboardTest() {
        when(dashboardRepository.findByDashboardUser(any(User.class))).thenReturn(null);

        Assertions.assertThrows(EntityNotFoundException.class,  ()-> {
            dashboardService.getDashboardItemData(1L, new User());
        });
    }

    @DisplayName("Invalid dashboard item type test")
    @Test
    public void invalidItemTypeTest() {
        Dashboard dashboard = new Dashboard();
        CapabilityTable table = new CapabilityTable();
        table.setType(ItemType.CAPABILITY_TABLE);
        dashboard.setDashboardItems(Arrays.asList(table));

        when(dashboardRepository.findByDashboardUser(any(User.class))).thenReturn(dashboard);
        Assertions.assertThrows(InvalidDataException.class,  ()-> {
            dashboardService.getDashboardItemData(1L, new User());
        });
    }

    @DisplayName("Non existing requested item test")
    @Test
    public void nonExistingItemTest() {
        Dashboard dashboard = new Dashboard();
        LevelBarGraph item = new LevelBarGraph();
        item.setType(ItemType.LEVEL_BAR_GRAPH);
        item.setId(1L);
        dashboard.setDashboardItems(Arrays.asList(item));

        when(dashboardRepository.findByDashboardUser(any(User.class))).thenReturn(dashboard);
        Assertions.assertThrows(EntityNotFoundException.class,  ()-> {
            dashboardService.getDashboardItemData(2L, new User());
        });
    }

    @DisplayName("Dashboard bar graph data")
    @Test
    public void getBarDataTest() {
        Dashboard dashboard = new Dashboard();
        LevelBarGraph item = new LevelBarGraph();
        item.setType(ItemType.LEVEL_BAR_GRAPH);
        item.setId(1L);
        dashboard.setDashboardItems(Arrays.asList(item));

        LinkedHashMap<String, LinkedHashMap<String, Integer>> dataMap = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> data = new LinkedHashMap<>();
        data.put("Assessor 1", 1);
        data.put("Assessor 2", 2);
        dataMap.put("Process 1", data);
        dataMap.put("Process 2", data);

        when(dashboardRepository.findByDashboardUser(any(User.class))).thenReturn(dashboard);
        doNothing().when(itemValidationService).validateItemWithValid(any(), any());
        when(levelBarGraphService.getData(any())).thenReturn(dataMap);

        List<Map<String, String>> result =  dashboardService.getDashboardItemData(1L, new User());
        Assertions.assertEquals(4, result.size());
        Assertions.assertTrue(result.get(0).containsKey("process"));
        Assertions.assertTrue(result.get(0).containsKey("assessor"));
        Assertions.assertTrue(result.get(0).containsKey("level"));
    }

    @DisplayName("Dashboard pie graph data")
    @Test
    public void getPieDataTest() {
        Dashboard dashboard = new Dashboard();
        LevelPieGraph item = new LevelPieGraph();
        item.setType(ItemType.LEVEL_PIE_GRAPH);
        item.setId(1L);
        dashboard.setDashboardItems(Arrays.asList(item));

        LinkedHashMap<String, Integer> dataMap = new LinkedHashMap<>();
        dataMap.put("Level 0", 1);
        dataMap.put("Level 2", 2);

        when(dashboardRepository.findByDashboardUser(any(User.class))).thenReturn(dashboard);
        doNothing().when(itemValidationService).validateItemWithValid(any(), any());
        when(levelPieGraphService.getData(any())).thenReturn(dataMap);

        List<Map<String, String>> result =  dashboardService.getDashboardItemData(1L, new User());
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.get(0).containsKey("level"));
        Assertions.assertTrue(result.get(0).containsKey("count"));
    }
}
