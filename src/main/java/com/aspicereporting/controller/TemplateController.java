package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.service.TemplateService;
import com.fasterxml.jackson.annotation.JsonView;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/templates")
public class TemplateController {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    TemplateService templateService;

    @PostMapping("/save")
    public Template createOrEditTemplate(@RequestBody Template template, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return templateService.saveOrEditTemplate(template, loggedUser);
    }

    @JsonView(View.Table.class)
    @GetMapping("/getAll")
    public List<Template> getAll(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return templateService.getAllByUser(loggedUser);
    }

    @JsonView(View.Simple.class)
    @GetMapping("/allSimple")
    public List<Template> getAllSimple(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return templateService.getAllByUser(loggedUser);
    }

    @JsonView(View.Canvas.class)
    @GetMapping("/get")
    public Template getById(@RequestParam Long templateId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return templateService.getTemplateById(templateId, loggedUser);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam Long templateId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        templateService.deleteTemplate(templateId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Template id= " + templateId + " deleted."));
    }

}
