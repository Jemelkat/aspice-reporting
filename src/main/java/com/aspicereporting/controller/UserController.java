package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.Group;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.service.UserService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping(value = "/addToGroup")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> addUserGroup(@RequestParam("groupId") Long groupId, @RequestParam(value = "userId", required = false) Long userId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        userService.addGroupToUser(groupId, userId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("User " + loggedUser.getUsername() + " added to group successfully."));
    }

    @PostMapping(value = "/edit")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> editUser(@RequestBody User user, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();

        if (((loggedUser.getId() != null) && (loggedUser.getId() == user.getId())) || (loggedUser.isAdmin())) {
            userService.update(user);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("You cannot edit other users without admin rights"));
        }

        return ResponseEntity.ok(new MessageResponse("User edited successfully."));
    }

    @PostMapping(value = "/delete")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> delete(@RequestParam Long id) {
        userService.delete(id);
        return ResponseEntity.ok(new MessageResponse("User id " + id + " deleted successfully."));
    }

    @JsonView(View.Simple.class)
    @GetMapping(value = "/groups")
    public List<Group> getAllGroups(Authentication authentication){
        User loggedUser = (User) authentication.getPrincipal();
        List<Group> test=  userService.getAllGroups(loggedUser);
        return test;
    }
}
