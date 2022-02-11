package com.aspicereporting.repository;

import com.aspicereporting.entity.Dashboard;
import com.aspicereporting.entity.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardRepository extends CrudRepository<Dashboard, Long> {
    Dashboard findByDashboardUser(User user);
}
