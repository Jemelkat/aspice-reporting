package com.aspicereporting.repository;

import com.aspicereporting.entity.SourceColumn;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SourceColumnRepository extends CrudRepository<SourceColumn, Long> {
    List<SourceColumn> findAllByIdIn(List<Long> ids);

    SourceColumn findFirstById(Long id);
}
