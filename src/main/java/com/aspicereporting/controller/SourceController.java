package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.User;
import com.aspicereporting.service.SourceService;
import com.aspicereporting.service.UserService;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/source")
public class SourceController {
    @Autowired
    private Mapper mapper;

    @Autowired
    private SourceService sourceService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadSource(@RequestParam("file") MultipartFile file, Authentication authentication) {
        User user = mapper.map(authentication.getPrincipal(), User.class);

        //Store multipart file as source
        sourceService.storeFileAsSource(file, user);

        String fileName = file.getOriginalFilename();
        return ResponseEntity.ok(new MessageResponse(fileName + "saved."));
    }

    @GetMapping("/getAll")
    public List<Source> getAllSources(Authentication authentication) {
        User user = mapper.map(authentication.getPrincipal(), User.class);
        //Get all sources for logged user
        return sourceService.getSourcesByUser(user);
    }

}
