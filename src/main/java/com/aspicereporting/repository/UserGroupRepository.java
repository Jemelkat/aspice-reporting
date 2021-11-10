package com.aspicereporting.repository;

import com.aspicereporting.entity.UserGroup;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGroupRepository extends CrudRepository<UserGroup, Long> {
    UserGroup getUserGroupById(Long id);
}
