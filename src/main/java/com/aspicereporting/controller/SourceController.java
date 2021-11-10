package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.User;
import com.aspicereporting.service.SourceService;
import com.aspicereporting.service.UserService;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/source")
public class SourceController {
    @Autowired
    private Mapper mapper;

    @Autowired
    private SourceService sourceService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadSource(@RequestParam MultipartFile file, Principal principal) {
        User user = mapper.map(principal, User.class);
        sourceService.storeFileAsSource(file, user);

        String fileName = file.getOriginalFilename();
        return ResponseEntity.ok(new MessageResponse(fileName + "saved."));
    }
}
