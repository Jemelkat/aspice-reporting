package com.aspicereporting.service;

import com.aspicereporting.entity.Role;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.entity.User;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.repository.RoleRepository;
import com.aspicereporting.repository.UserGroupRepository;
import com.aspicereporting.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.aspicereporting.entity.Role.ERole.ROLE_USER;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    RoleRepository roleRepository;

    @Transactional
    public void addGroupToUser(Long groupId, Long userId, User user) {
        //Get userGroupFromDb
        UserGroup userGroup = userGroupRepository.getUserGroupById(groupId);
        if (userGroup == null) {
            throw new EntityNotFoundException("Could not find user group.");
        }

        //Use existing user or logger user
        if (userId != null) {
            Optional<User> foundUser = userRepository.findById(userId);
            if (foundUser.isPresent()) {
                user = foundUser.get();
            } else {
                throw new EntityNotFoundException("Could not find user with id " + userId);
            }
        }

        userGroup.addUser(user);
        userRepository.save(user);
    }

    public void updateUser(User user) {
        //If ID is not defined we cannot update the user
        if (user.getId() == null) {
            throw new EntityNotFoundException("Cannot update user without ID.");
        }

        //Get user from DB for updates
        User currentUser;
        try {
            currentUser = userRepository.findById(user.getId()).get();
        } catch (NoSuchElementException e) {
            throw new EntityNotFoundException("Could not find user with id " + user.getId(), e);
        }

        //Set new unsername and email
        currentUser.setUsername(user.getUsername());
        currentUser.setEmail(user.getEmail());
        List<Role.ERole> roleNames = user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toList());
        Set<Role> roles = roleRepository.findAllByNameIn(roleNames);
        Optional<Role> userRole = roleRepository.findByName(ROLE_USER);
        if(userRole.isEmpty()) {
            throw new EntityNotFoundException("Default user role USER_ROLE not found.");
        }
        roles.add(userRole.get());
        currentUser.setRoles(roles);

        userRepository.save(currentUser);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.findById(userId).ifPresentOrElse((obj) -> {
            for(Source source : obj.getSources()) {
                source.prepareForDelete();
            }
            userRepository.delete(obj);
        }, () -> {
            throw new EntityNotFoundException("User id " + userId + " not found.");
        });
    }

    public List<UserGroup> getGroupsForUser(User user) {
        return userGroupRepository.findAllByUsersContains(user);
    }
}
