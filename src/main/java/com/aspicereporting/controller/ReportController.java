package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Report;
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

    @PostMapping(value = "/create")
    public ResponseEntity<?> createReport(@RequestBody Report report, Authentication authentication) {
        User loggedUser = mapper.map(authentication.getPrincipal(), User.class);
        report.setReportUser(loggedUser);

        //Reconstruct
        for(ReportItem r: report.getReportItems()) {
            r.setReport(report);
        }
        reportRepository.save(report);

        return ResponseEntity.ok(new MessageResponse("Report " + report.getReportId() + " created successfully."));
    }

    @GetMapping(value = "/getAll")
    public List<Report> getAllReports(Authentication authentication) {
        User loggedUser = mapper.map(authentication.getPrincipal(), User.class);
        //Get all reports for logged user
        return reportService.getAllReportsByUser(loggedUser);
    }
}
