package com.aspicereporting.repository;

import com.aspicereporting.entity.Group;
import com.aspicereporting.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupRepository extends JpaRepository<Group, Long> {
    Group getUserGroupById(Long id);
    @Override
    List<Group> findAll();
    @Override
    Optional<Group> findById(Long id);
    List<Group> findAllByIdIn(List<Long> ids);
    Group findByUsersContains(User user);
    List<Group> findAllByUsersContains(User user);
}
