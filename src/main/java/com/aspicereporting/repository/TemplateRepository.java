package com.aspicereporting.repository;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findDistinctByTemplateUserOrTemplateGroupsIn(User user, Set<UserGroup> userGroups);
    Template findByTemplateId(Long templateId);
    Template findByTemplateUserAndTemplateId(User user, Long id);
    Template save(Template template);
}
