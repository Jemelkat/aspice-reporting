package com.aspicereporting.repository;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.items.ReportItem;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ReportItemsRepository extends CrudRepository<ReportItem, Long> {
    List<ReportItem> findAllByReportAndIdIn(Report report, Set<Long> ids);
}
