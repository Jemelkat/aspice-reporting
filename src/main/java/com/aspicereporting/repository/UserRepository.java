package com.aspicereporting.repository;

import com.aspicereporting.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

    List<User> findByIdIn(List<Long> ids);

    User getById(Long id);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    @Override
    List<User> findAll();
}
