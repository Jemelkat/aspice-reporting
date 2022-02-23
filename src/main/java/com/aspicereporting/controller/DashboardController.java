package com.aspicereporting.controller;

import com.aspicereporting.entity.Dashboard;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.service.DashboardService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@CrossOrigin
@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    @Autowired
    DashboardService dashboardService;

    @JsonView(View.Canvas.class)
    @PostMapping("/save")
    public Dashboard save(@RequestBody Dashboard dashboard, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return dashboardService.saveDashboard(dashboard, loggedUser);
    }

    @JsonView(View.Canvas.class)
    @GetMapping("")
    public Dashboard getById(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return dashboardService.getDashboardByUser(loggedUser);
    }

    @GetMapping("/data")
    public List<Map<String, String>> getItemData(@RequestParam Long itemId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return dashboardService.getDashboardItemData(itemId, loggedUser);
    }
}
