package com.aspicereporting.repository;

import com.aspicereporting.entity.Group;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface SourceRepository extends CrudRepository<Source, Long> {
    List<Source> findDistinctByUserOrSourceGroupsIn(User user, Set<Group> userGroups);
    Source findBySourceId(Long sourceId);
    Source findBySourceIdAndUser(Long sourceId, User user);
    @Query("select distinct s from Source s left join s.sourceGroups g WHERE s.sourceId=:sourceId and (s.user = :user or g in (:userGroups))")
    Source findBySourceIdAndUserOrSourceGroupsIn(@Param("sourceId") Long sourceId, @Param("user") User user, @Param("userGroups") Set<Group> userGroups);
}
