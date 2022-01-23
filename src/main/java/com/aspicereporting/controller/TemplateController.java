package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.dto.TemplateTableDTO;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.service.TemplateService;
import com.fasterxml.jackson.annotation.JsonView;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        //Edit old or create new template
        return templateService.saveOrEditTemplate(template, loggedUser);
    }

    @GetMapping("/getAll")
    public List<TemplateTableDTO> getAll(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        List<Template> templates = templateService.getAllByUserOrShared(loggedUser);

        //Convert Entity to custom DTO
        return templates.stream().map((s) -> {
            TemplateTableDTO sDTO = modelMapper.map(s, TemplateTableDTO.class);
            if (!s.getTemplateGroups().isEmpty()) {
                sDTO.setShared(Boolean.TRUE);
                sDTO.setSharedBy(s.getTemplateUser().getId() == loggedUser.getId() ? "You" : s.getTemplateUser().getUsername());
            }
            return sDTO;
        }).collect(Collectors.toList());
    }

    @JsonView(View.Canvas.class)
    @GetMapping("/get")
    public Template getById(@RequestParam Long templateId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return templateService.getTemplateById(templateId, loggedUser);
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareWithGroups(@PathVariable("id") Long templateId, @RequestBody List<Long> groupIds, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        templateService.shareWithGroups(templateId, groupIds, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Template id= " + templateId + " shared."));
    }

    @JsonView(View.Simple.class)
    @GetMapping("/{id}/groups")
    public Set<UserGroup> getGroups(@PathVariable("id") Long templateId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return templateService.getGroupsForTemplate(templateId, loggedUser);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam Long templateId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        templateService.deleteTemplate(templateId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Template id= " + templateId + " deleted."));
    }

}
