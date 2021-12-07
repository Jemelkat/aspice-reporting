package com.aspicereporting.repository.jdbc.dao;

import com.aspicereporting.entity.User;

import java.util.List;

public interface UserDao {
    void update(User user);
    User findById(Long id);
    List<User> findAllById(List<Long> groupId);
}
