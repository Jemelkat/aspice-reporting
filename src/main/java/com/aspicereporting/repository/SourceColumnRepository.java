package com.aspicereporting.repository;

import com.aspicereporting.entity.SourceColumn;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

public interface SourceColumnRepository extends CrudRepository<SourceColumn, Long> {
    List<SourceColumn> findAllByIdIn(List<Long> ids);
}
