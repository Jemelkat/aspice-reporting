package com.aspicereporting.repository;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface SourceRepository extends CrudRepository<Source, Long> {
    List<Source> findDistinctByUserOrSourceGroupsIn(User user, Set<UserGroup> userGroups);

    Source findFirstById(Long sourceId);

    Source findByIdAndUser(Long sourceId, User user);

    @Query("select distinct s from Source s left join s.sourceGroups g WHERE s.id=:sourceId and (s.user = :user or g in (:userGroups))")
    Source findByIdAndUserOrSourceGroupsIn(@Param("sourceId") Long sourceId, @Param("user") User user, @Param("userGroups") Set<UserGroup> userGroups);

    @Query("select distinct s from Source s left join s.sourceGroups g WHERE s.id in (:sources) and (s.user = :user or g in (:userGroups))")
    List<Source> findByIdInAndUserOrSourceGroupsIn(@Param("sources") Set<Long> sources, @Param("user") User user, @Param("userGroups") Set<UserGroup> userGroups);


    @Query("Select distinct sd.value from SourceColumn sc join SourceData sd on sc.id=sd.sourceColumn.id where sc.id=?1")
    List<String> findDistinctColumnValuesForColumn(Long id);
}
