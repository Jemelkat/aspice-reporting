package com.aspicereporting.service;

import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.repository.UserGroupRepository;
import com.aspicereporting.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserGroupService {

    @Autowired
    UserGroupRepository userGroupRepository;

    @Autowired
    UserRepository userRepository;

    public void updateUserGroup(UserGroup userGroup) {
        if (userGroup.getId() == null) {
            throw new EntityNotFoundException("Cannot update user group " + userGroup.getGroupName() + " no id provided.");
        }
        //Get current group by id
        UserGroup oldUserGroup = userGroupRepository.findById(userGroup.getId())
                .orElseThrow(() -> new EntityNotFoundException("Could not find user group with id " +userGroup.getId()  ));

        //Get users from ids in updated group
        List<User> newUsersList = userRepository.findByIdIn(userGroup.getUsers()
                .stream()
                .map(User::getId)
                .collect(Collectors.toList()));

        //Set new values
        oldUserGroup.setGroupName(userGroup.getGroupName());
        oldUserGroup.setUsers(newUsersList);

        userGroupRepository.save(oldUserGroup);
    }

    public void deleteUserGroup(Long userGroupId) {
        Optional<UserGroup> userGroup = userGroupRepository.findById(userGroupId);

        //Delete only if group exists
        userGroup.ifPresentOrElse(
                (obj) -> {
                    //Handle many to many relation
                    for (User u : obj.getUsers()) {
                        u.setUserGroup(null);
                    }
                    for (Template t : obj.getTemplates()) {
                        t.removeGroup();
                    }
                    obj.getTemplates().clear();

                    userGroupRepository.delete(obj);
                },
                () -> {
                    throw new EntityNotFoundException("User group id " + userGroupId + " not found.");
                }
        );
    }

    public List<UserGroup> getAllUserGroups() {
        return userGroupRepository.findAll();
    }

    public void createUserGroup(UserGroup group) {
        //Get group user ids
        List<Long> userIds = new ArrayList<>();
        if (!group.getUsers().isEmpty()) {
            userIds = group.getUsers()
                    .stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
        }

        //Get user entities
        List<User> users = userRepository.findByIdIn(userIds);
        //Add user entities to group
        group.setUsers(users);
        //Reconstruct
        for (User u : users) {
            u.setUserGroup(group);
        }

        userGroupRepository.save(group);
    }
}
