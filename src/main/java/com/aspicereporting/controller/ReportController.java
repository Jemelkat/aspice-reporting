package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.dto.ReportTableDTO;
import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.service.JasperService;
import com.aspicereporting.service.ReportService;
import com.fasterxml.jackson.annotation.JsonView;
import net.sf.jasperreports.engine.JRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin()
@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ReportService reportService;

    @Autowired
    JasperService jasperService;

    @GetMapping(value = "/getAll")
    public List<ReportTableDTO> getAll(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        List<Report> reports = reportService.getAllByUserOrShared(loggedUser);

        //Convert Entity to custom DTO
        return reports.stream().map((s) -> {
            ReportTableDTO sDTO = modelMapper.map(s, ReportTableDTO.class);
            if (!s.getReportGroups().isEmpty()) {
                sDTO.setShared(Boolean.TRUE);
                sDTO.setSharedBy(s.getReportUser().getId() == loggedUser.getId() ? "You" : s.getReportUser().getUsername());
            }
            return sDTO;
        }).collect(Collectors.toList());
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
    public Report getById(@RequestParam Long reportId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return reportService.getReportById(reportId, loggedUser);
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareWithGroups(@PathVariable("id") Long reportId, @RequestBody List<Long> groupIds, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        reportService.shareWithGroups(reportId, groupIds, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Report id= " + reportId + " shared."));
    }

    @JsonView(View.Simple.class)
    @GetMapping("/{id}/groups")
    public Set<UserGroup> getGroups(@PathVariable("id") Long reportId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return reportService.getGroupsForReport(reportId, loggedUser);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam Long reportId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        reportService.deleteReport(reportId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Report id= " + reportId + " deleted."));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestParam Long reportId, Authentication authentication) throws JRException, ClassNotFoundException {
        User loggedUser = (User) authentication.getPrincipal();
        Report report = reportService.getReportById(reportId, loggedUser);
        jasperService.generateReport(report);
        return ResponseEntity.ok(new MessageResponse("Report generated."));
    }
}
