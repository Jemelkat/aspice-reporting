package com.aspicereporting.controller;

import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.service.UserGroupService;
import com.aspicereporting.service.UserService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserGroupService userGroupService;

    @JsonView(View.Detailed.class)
    @GetMapping(value = "/getAllUsers")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @JsonView(View.Detailed.class)
    @GetMapping(value = "/getAllGroups")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<UserGroup> getAllGroups() {
        return userGroupService.getAllUserGroups();
    }
}
