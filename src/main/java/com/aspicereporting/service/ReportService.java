package com.aspicereporting.service;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.User;
import com.aspicereporting.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportService {
    @Autowired
    ReportRepository reportRepository;

    public List<Report> getAllReportsByUser(User user) {
        return reportRepository.findAllByReportUser(user);
    }
}
