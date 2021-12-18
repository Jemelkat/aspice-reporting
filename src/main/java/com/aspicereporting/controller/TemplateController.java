package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.dto.SimpleGroupDTO;
import com.aspicereporting.dto.SimpleUserDTO;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.service.TemplateService;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
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
    public ResponseEntity<?> createOrEditTemplate(@RequestBody Template template, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        //Edit old or create new template
        templateService.saveOrEditTemplate(template, loggedUser);
        return ResponseEntity.ok(new MessageResponse(template.getTemplateName() + "saved."));
    }

    @JsonView(View.SimpleTable.class)
    @GetMapping("/getAll")
    public List<Template> getAllTemplates(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        List<Template> templates = templateService.getAllTemplatesByUser(loggedUser);

        //Get all templates for logged user
        return templates;
    }

    @JsonView(View.Canvas.class)
    @GetMapping("/get")
    public Template getTemplateById(@RequestParam Long templateId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return templateService.getTemplateById(templateId, loggedUser);
    }

    @PostMapping("/share")
    public ResponseEntity<?> shareTemplateWithGroup(@RequestParam Long templateId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        templateService.shareTemplate(templateId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Template id= " + templateId + " shared with your group."));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteTemplate(@RequestParam Long templateId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        templateService.deleteTemplate(templateId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Template id= " + templateId + " deleted."));
    }

}
