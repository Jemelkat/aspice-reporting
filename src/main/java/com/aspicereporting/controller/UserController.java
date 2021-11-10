package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.User;
import com.aspicereporting.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping(value = "/addToGroup")
    public ResponseEntity<?> addUserGroup(@RequestParam("groupId") Long groupId, @RequestParam(value = "userId", required = false) Long userId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        userService.addGroupToUser(groupId, userId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("User " + loggedUser.getUsername() + " added to group successfully."));
    }
}
