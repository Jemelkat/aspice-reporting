package com.aspicereporting.controller;

import com.aspicereporting.controller.response.MessageResponse;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.service.UserService;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    Mapper mapper;

    @PostMapping(value = "/addToGroup")
    public ResponseEntity<?> addUserGroup(@RequestParam("groupId") Long groupId, @RequestParam(value = "userId", required = false) Long userId, Authentication authentication) {
        User loggedUser = mapper.map(authentication.getPrincipal(), User.class);
        userService.addGroupToUser(groupId, userId, loggedUser);
        return ResponseEntity.ok(new MessageResponse("User " + loggedUser.getUsername() + " added to group successfully."));
    }

    @PostMapping(value ="/edit")
    public ResponseEntity<?> editUser(@RequestBody User user, Authentication authentication) {
        User loggedUser = mapper.map(authentication.getPrincipal(), User.class);

        if(((loggedUser.getId() != null) && (loggedUser.getId() == user.getId())) || (loggedUser.isAdmin())) {
            userService.update(user);
        }
        else {
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
}
