package com.aspicereporting.controller;

import com.aspicereporting.dto.MessageResponseDTO;
import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.service.JasperService;
import com.aspicereporting.service.ReportService;
import com.fasterxml.jackson.annotation.JsonView;
import net.sf.jasperreports.engine.JRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ReportService reportService;

    @Autowired
    JasperService jasperService;

    @JsonView(View.Table.class)
    @GetMapping(value = "/getAll")
    public List<Report> getAll(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return reportService.getAllByUser(loggedUser);
    }

    @JsonView(View.Simple.class)
    @GetMapping(value = "/allSimple")
    public List<Report> getAllSimple(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return reportService.getAllByUser(loggedUser);
    }

    @JsonView(View.Canvas.class)
    @PostMapping("/save")
    public Report createOrEditReport(@RequestBody Report report, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        //Edit old or create new template
        return reportService.saveOrEdit(report, loggedUser);
    }

    @JsonView(View.Canvas.class)
    @GetMapping("/get")
    public Report getById(@RequestParam Long reportId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return reportService.getReportById(reportId, loggedUser);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam Long reportId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        reportService.deleteReport(reportId, loggedUser);
        return ResponseEntity.ok(new MessageResponseDTO("Report id= " + reportId + " deleted."));
    }

    @GetMapping("/generate")
    public ResponseEntity<?> generate(@RequestParam Long reportId, Authentication authentication) throws JRException {
        User loggedUser = (User) authentication.getPrincipal();
        //Get report PDF as byte array stream
        ByteArrayOutputStream out = reportService.generateReportById(reportId, loggedUser);

        ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + reportId + ".pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
