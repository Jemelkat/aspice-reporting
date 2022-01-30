package com.aspicereporting.repository;

import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Set;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findDistinctByTemplateUserOrTemplateGroupsIn(User templateUser, Set<UserGroup> userGroups);

    Template findFirstById(Long templateId);

    Template findByTemplateUserAndId(User user, Long id);
    Template findByTemplateUserAndIdOrTemplateGroupsIn(User templateUser, Long id, Set<UserGroup> userGroups);

    Template save(Template template);
}
