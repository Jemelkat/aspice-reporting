package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.repository.ReportRepository;
import com.aspicereporting.service.JasperService;
import com.aspicereporting.service.ReportService;
import com.fasterxml.jackson.annotation.JsonView;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    ReportService reportService;

    @Autowired
    JasperService jasperService;

    @JsonView(View.Simple.class)
    @GetMapping(value = "/getAll")
    public List<Report> getAllReports(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return reportService.getAllReportsForUser(loggedUser);
    }

    @JsonView(View.Canvas.class)
    @PostMapping("/save")
    public Report createOrEditReport(@RequestBody @Valid Report report, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        //Edit old or create new template
        return reportService.saveOrEditReport(report, loggedUser);
    }

    @JsonView(View.Canvas.class)
    @GetMapping("/get")
    public Report getReportById(@RequestParam Long reportId, Authentication authentication) {
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
    public ResponseEntity<?> deleteReport(@RequestParam Long reportId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        reportService.deleteReport(reportId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Report id= " + reportId + " deleted."));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateReport(@RequestParam Long reportId, Authentication authentication) throws JRException, ClassNotFoundException {
        User loggedUser = (User) authentication.getPrincipal();
        Report report = reportService.getReportById(reportId, loggedUser);
        jasperService.generateReport(report);
        return ResponseEntity.ok(new MessageResponse("Report generated."));
    }
}
