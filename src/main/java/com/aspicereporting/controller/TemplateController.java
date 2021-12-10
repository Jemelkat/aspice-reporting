package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.service.TemplateService;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/template")
public class TemplateController {

    @Autowired
    TemplateService templateService;

    @Autowired
    Mapper mapper;

    @PostMapping("/save")
    public ResponseEntity<?> createOrEditTemplate(@RequestBody Template template, Authentication authentication) {
        User loggedUser = mapper.map(authentication.getPrincipal(), User.class);
        //Edit old or create new template
        templateService.saveOrEditTemplate(template, loggedUser);
        return ResponseEntity.ok(new MessageResponse(template.getTemplateName() + "saved."));
    }

    @GetMapping("/getAll")
    public List<Template> getAllTemplates(Authentication authentication) {
        User loggedUser = mapper.map(authentication.getPrincipal(), User.class);
        //Get all templates for logged user
        return templateService.getAllTemplatesByUser(loggedUser);
    }

}
