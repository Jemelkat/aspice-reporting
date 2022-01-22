package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Group;
import com.aspicereporting.service.UserGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@CrossOrigin
@RestController
@RequestMapping("/group")
public class GroupController {

    @Autowired
    UserGroupService userGroupService;

    @PostMapping(value = "/edit")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> editUserGroup(@RequestBody @Valid Group group) throws Exception {
        userGroupService.updateUserGroup(group);
        return ResponseEntity.ok(new MessageResponse("User group " + group.getGroupName() + " edited successfully."));
    }

    @DeleteMapping(value = "/delete")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteUserGroup(@RequestParam("id") Long id) {
        userGroupService.deleteUserGroup(id);
        return ResponseEntity.ok(new MessageResponse("User group id " + id + " deleted successfully."));
    }


    @PostMapping(value = "/create")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> createUserGroup(@RequestBody @Valid Group group) {
        userGroupService.createUserGroup(group);
        return ResponseEntity.ok(new MessageResponse("User group id " + group.getId() + " created successfully."));
    }
}
