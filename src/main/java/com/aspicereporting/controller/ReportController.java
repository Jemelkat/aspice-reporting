package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.repository.ReportRepository;
import com.aspicereporting.service.ReportService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/report")
public class ReportController {
    @Autowired
    ReportRepository reportRepository;

    @Autowired
    ReportService reportService;

    @JsonView(View.Simple.class)
    @GetMapping(value = "/getAll")
    public List<Report> getAllReports(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        //Get all reports for logged user
        return reportService.getAllReportsForUser(loggedUser);
    }

    @PostMapping("/save")
    public ResponseEntity<?> createOrEditReport(@RequestBody Report report, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        //Edit old or create new template
        reportService.saveOrEditReport(report, loggedUser);
        return ResponseEntity.ok(new MessageResponse(report.getReportName() + "saved."));
    }

    @JsonView(View.Canvas.class)
    @GetMapping("/get")
    public Report getReportById(@RequestParam Long reportId, Authentication authentication){
        User loggedUser = (User) authentication.getPrincipal();
        return reportService.getReportById(reportId, loggedUser);
    }

    @PostMapping("/share")
    public ResponseEntity<?> shareReportWithGroup(@RequestParam Long reportId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        reportService.shareReport(reportId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Report id= " + reportId + " shared with your group."));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteReport(@RequestParam Long reportId, Authentication authentication){
        User loggedUser = (User) authentication.getPrincipal();
        reportService.deleteReport(reportId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Report id= " + reportId + " deleted."));
    }
}
