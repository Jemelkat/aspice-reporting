package com.aspicereporting.repository;

import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
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

    UserGroup findByUsersContains(User user);
}
