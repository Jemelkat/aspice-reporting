package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.User;
import com.aspicereporting.repository.ReportRepository;
import com.aspicereporting.service.ReportService;
import org.dozer.Mapper;
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

    @Autowired
    Mapper mapper;

    @GetMapping(value = "/getAll")
    public List<Report> getAllReports(Authentication authentication) {
        User loggedUser = mapper.map(authentication.getPrincipal(), User.class);
        //Get all reports for logged user
        return reportService.getAllReportsByUser(loggedUser);
    }

    @PostMapping("/save")
    public ResponseEntity<?> createOrEditReport(@RequestBody Report report, Authentication authentication) {
        User loggedUser = mapper.map(authentication.getPrincipal(), User.class);
        //Edit old or create new template
        reportService.saveOrEditReport(report, loggedUser);
        return ResponseEntity.ok(new MessageResponse(report.getReportName() + "saved."));
    }

    @GetMapping("/get")
    public Report getReportById(@RequestParam Long reportId, Authentication authentication){
        User loggerUser = mapper.map(authentication.getPrincipal(), User.class);
        return reportService.getReportById(reportId, loggerUser);
    }
}
