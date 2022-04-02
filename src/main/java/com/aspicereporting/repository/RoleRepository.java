package com.aspicereporting.repository;

import com.aspicereporting.entity.Role;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends CrudRepository<Role, Long> {
    Optional<Role> findByName(Role.ERole name);
    Set<Role> findAllByNameIn(List<Role.ERole> roles);
}
