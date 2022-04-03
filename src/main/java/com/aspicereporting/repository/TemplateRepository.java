package com.aspicereporting.repository;

import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findAllByTemplateUser(User user);
    Template findByTemplateUserAndId(User user, Long id);

    Template save(Template template);
}
