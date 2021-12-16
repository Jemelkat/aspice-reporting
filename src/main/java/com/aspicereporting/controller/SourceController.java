package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.User;
import com.aspicereporting.service.SourceService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/getAll")
    public List<Source> getAllSources(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        //Get all sources for logged user
        return sourceService.getSourcesByUser(loggedUser);
    }

}
