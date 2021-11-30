package com.aspicereporting.repository;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SourceRepository extends CrudRepository<Source, Long> {
    List<Source> findAllByUser(User user);
}
