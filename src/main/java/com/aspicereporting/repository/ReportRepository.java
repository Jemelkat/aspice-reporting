package com.aspicereporting.repository;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ReportRepository extends CrudRepository<Report, Long> {
    List<Report> findAllByReportUser(User user);

    Report findByIdAndReportUser(Long id, User user);

    Report save(Report report);
}
