package com.aspicereporting.service;

import com.aspicereporting.entity.*;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.repository.UserGroupRepository;
import com.aspicereporting.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserGroupService {

    @Autowired
    UserGroupRepository userGroupRepository;

    @Autowired
    UserRepository userRepository;

    public void updateUserGroup(UserGroup updatedGroup) throws Exception {
        if (updatedGroup.getId() == null) {
            throw new EntityNotFoundException("Cannot update user group " + updatedGroup.getGroupName() + " no id provided.");
        }
        //Get current group by id
        UserGroup currentGroup = userGroupRepository.findById(updatedGroup.getId()).orElseThrow(() -> new EntityNotFoundException("Could not find user group with id " + updatedGroup.getId()));

        //Get and remove all removed users from this group
        Set<User> removedUsers = new HashSet<>(currentGroup.getUsers());
        removedUsers.removeAll(updatedGroup.getUsers());

        for (User user : removedUsers) {
            user.removeUserGroup(updatedGroup);
        }

        //Get users from ids in updated group entity
        List<User> updatedUsersList = userRepository.findByIdIn(updatedGroup.getUsers().stream().map(User::getId).collect(Collectors.toList()));

        //Update group name
        currentGroup.setGroupName(updatedGroup.getGroupName());
        //Set new users to the group
        for (User u : updatedUsersList) {
            u.addUserGroup(currentGroup);
        }

        userGroupRepository.save(currentGroup);
    }

    public void deleteUserGroup(Long userGroupId) {
        Optional<UserGroup> userGroup = userGroupRepository.findById(userGroupId);

        //Delete only if group exists
        userGroup.ifPresentOrElse((group) -> {
            //Handle many to many relations
            for (User u : new HashSet<>(group.getUsers())) {
                u.removeUserGroup(group);
            }
            for (Source s : new HashSet<>(group.getSources())) {
                s.removeGroup(group);
            }
            for (Report r : new HashSet<>(group.getReports())) {
                r.removeGroup(group);
            }


            //Delete group
            userGroupRepository.delete(group);
        }, () -> {
            throw new EntityNotFoundException("User group id " + userGroupId + " not found.");
        });
    }

    public List<UserGroup> getAllUserGroups() {
        return userGroupRepository.findAll();
    }

    public void createUserGroup(UserGroup group) {
        //Get group user ids
        List<Long> userIds = new ArrayList<>();
        if (!group.getUsers().isEmpty()) {
            userIds = group.getUsers().stream().map(User::getId).collect(Collectors.toList());
        }

        //Get user entities
        List<User> users = userRepository.findByIdIn(userIds);
        //Add user entities to group
        for (User u : users) {
            u.addUserGroup(group);
        }

        userGroupRepository.save(group);
    }
}
