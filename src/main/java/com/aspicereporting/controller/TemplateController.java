package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.service.TemplateService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/template")
public class TemplateController {

    @Autowired
    TemplateService templateService;

    @PostMapping("/save")
    public Template createOrEditTemplate(@RequestBody Template template, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        //Edit old or create new template
        return templateService.saveOrEditTemplate(template, loggedUser);
    }

    @JsonView(View.SimpleTable.class)
    @GetMapping("/getAll")
    public List<Template> getAll(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        List<Template> templates = templateService.getAllTemplatesByUser(loggedUser);

        //Get all templates for logged user
        return templates;
    }

    @JsonView(View.Canvas.class)
    @GetMapping("/get")
    public Template getById(@RequestParam Long templateId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return templateService.getById(templateId, loggedUser);
    }

    @PostMapping("/share")
    public ResponseEntity<?> shareWithGroup(@RequestParam Long templateId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        templateService.shareWithGroups(templateId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Template id= " + templateId + " shared with your group."));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam Long templateId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        templateService.delete(templateId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Template id= " + templateId + " deleted."));
    }

}
