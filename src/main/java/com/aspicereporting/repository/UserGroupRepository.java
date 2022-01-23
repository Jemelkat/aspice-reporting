package com.aspicereporting.repository;

import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    UserGroup getUserGroupById(Long id);

    @Override
    List<UserGroup> findAll();

    @Override
    Optional<UserGroup> findById(Long id);

    List<UserGroup> findAllByIdIn(List<Long> ids);

    UserGroup findByUsersContains(User user);

    List<UserGroup> findAllByUsersContains(User user);
}
