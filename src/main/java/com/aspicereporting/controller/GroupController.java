package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.repository.UserGroupRepository;
import com.aspicereporting.service.UserGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/group")
public class GroupController {

    @Autowired
    UserGroupService userGroupService;

    @PostMapping(value = "/add")
    public ResponseEntity<?> addUserGroup(@Valid UserGroup userGroup) {
        userGroupService.addOrUpdateUserGroup(userGroup);
        return ResponseEntity.ok(new MessageResponse("User group " + userGroup.getGroupName() + " added successfully."));
    }

    @PostMapping(value = "/update")
    public ResponseEntity<?> updateUserGroup(@Valid UserGroup userGroup) {
        userGroupService.addOrUpdateUserGroup(userGroup);
        return ResponseEntity.ok(new MessageResponse("User group " + userGroup.getGroupName() + " updated successfully."));
    }

    @PostMapping(value = "/delete")
    public ResponseEntity<?> deleteUserGroup(@RequestParam("userGroupId") Long userGroupId) {
        userGroupService.deleteUserGroup(userGroupId);
        return ResponseEntity.ok(new MessageResponse("User group id " + userGroupId + " deleted successfully."));
    }

}
