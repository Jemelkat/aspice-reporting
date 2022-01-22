package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.service.SourceService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/source")
public class SourceController {

    @Autowired
    private SourceService sourceService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadSource(@RequestParam("file") MultipartFile file, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();

        //Store multipart file as source
        sourceService.storeFileAsSource(file, loggedUser);

        String fileName = file.getOriginalFilename();
        return ResponseEntity.ok(new MessageResponse(fileName + "saved."));
    }

    @JsonView(View.Simple.class)
    @GetMapping("/getAll")
    public List<Source> getAllSources(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return sourceService.getSourcesByUser(loggedUser);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteSource(@Param("sourceId") Long sourceId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        sourceService.deleteSourceById(sourceId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Source id=" + sourceId + " deleted."));
    }
}
