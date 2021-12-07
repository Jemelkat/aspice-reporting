package com.aspicereporting.repository;

import com.aspicereporting.entity.Report;
import org.springframework.data.repository.CrudRepository;

public interface ReportRepository extends CrudRepository<Report, Long> {
}
