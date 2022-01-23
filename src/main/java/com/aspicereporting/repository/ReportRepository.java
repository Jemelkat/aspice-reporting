package com.aspicereporting.repository;

import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

public interface ReportRepository extends CrudRepository<Report, Long> {
    List<Report> findDistinctByReportUserOrReportGroupsIn(User user, Set<UserGroup> userGroups);

    Report findByIdAndReportUser(Long id, User user);

    Report save(Report report);

    Report findFirstById(Long reportId);
}
