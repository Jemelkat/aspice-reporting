package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.dto.SourceTableDTO;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.service.SourceService;
import com.fasterxml.jackson.annotation.JsonView;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/source")
public class SourceController {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private SourceService sourceService;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();

        //Store multipart file as source
        sourceService.storeFileAsSource(file, loggedUser);

        return ResponseEntity.ok(new MessageResponse(file.getOriginalFilename() + "saved."));
    }

    @GetMapping("/getAll")
    public List<SourceTableDTO> getAll(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        List<Source> sourcesList = sourceService.getAllByUserOrShared(loggedUser);

        //Convert Entity to custom DTO
        return sourcesList
                .stream()
                .map((s) -> {
                    SourceTableDTO sDTO = modelMapper.map(s, SourceTableDTO.class);
                    if (!s.getSourceGroups().isEmpty()) {
                        sDTO.setShared(Boolean.TRUE);
                        sDTO.setSharedBy(s.getUser().getId() == loggedUser.getId() ? "You" : s.getUser().getUsername());
                    }
                    return sDTO;
                })
                .collect(Collectors.toList());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@Param("sourceId") Long sourceId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        sourceService.deleteById(sourceId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Source id=" + sourceId + " deleted."));
    }

    @JsonView(View.Simple.class)
    @GetMapping("/{id}/groups")
    public Set<UserGroup> getGroups(@PathVariable("id") Long sourceId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return sourceService.getGroupsForSource(sourceId, loggedUser);
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<?> share(@PathVariable("id") Long sourceId, @RequestBody List<Long> groupIds, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        sourceService.shareWithGroups(sourceId, groupIds, loggedUser);
        return ResponseEntity.ok(new MessageResponse("Source id=" + sourceId + " shared."));
    }
}
