package com.aspicereporting.service;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.exception.ConstraintException;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.repository.UserGroupRepository;
import com.aspicereporting.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
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

        try {
            userGroupRepository.save(currentGroup);
        } catch (DataIntegrityViolationException e) {
            if (e.getMostSpecificCause().getClass().getName().equals("org.postgresql.util.PSQLException") && ((SQLException) e.getMostSpecificCause()).getSQLState().equals("23505"))
                throw new ConstraintException("There is already group with this name.", e.getMostSpecificCause());
            throw new InvalidDataException("Error updating group", e);
        }
    }

    public void deleteUserGroup(Long userGroupId) {
        Optional<UserGroup> userGroup = userGroupRepository.findById(userGroupId);

        //Delete only if group exists
        userGroup.ifPresentOrElse((group) -> {
            //Handle many to many relations
            for (User user : new HashSet<>(group.getUsers())) {
                //Remove user group from user and from all reports/dashboards/templates
                user.removeUserGroup(group);
                //Remove sharing of sources to this deleted group
                for (Source s : new HashSet<>(user.getSources())) {
                    s.removeGroup(group);
                }
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
        group.setId(null);
        try {
            userGroupRepository.save(group);
        } catch (DataIntegrityViolationException e) {
            if (e.getMostSpecificCause().getClass().getName().equals("org.postgresql.util.PSQLException") && ((SQLException) e.getMostSpecificCause()).getSQLState().equals("23505"))
                throw new ConstraintException("There is already group with this name.", e.getMostSpecificCause());
            throw new InvalidDataException("Error saving group", e);
        }
    }
}
