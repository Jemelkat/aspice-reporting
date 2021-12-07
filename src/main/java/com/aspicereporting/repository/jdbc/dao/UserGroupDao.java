package com.aspicereporting.repository.jdbc.dao;

import com.aspicereporting.entity.UserGroup;

import java.util.List;

public interface UserGroupDao {
    List<UserGroup> getAll();
    UserGroup findById(Long id);
    void insert(UserGroup userGroup);
    void update(UserGroup userGroup);
}
