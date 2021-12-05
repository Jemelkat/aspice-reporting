package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.ReportItem;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.repository.ReportRepository;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/report")
public class ReportController {
    @Autowired
    ReportRepository reportRepository;

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
}
