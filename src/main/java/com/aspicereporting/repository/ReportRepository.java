package com.aspicereporting.repository;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.Group;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ReportRepository extends CrudRepository<Report, Long> {
    List<Report> findAllByReportUserOrReportGroup(User user, Group reportGroup);
    Report findByReportIdAndReportUser(Long id, User user);
    Report save(Report report);
}
