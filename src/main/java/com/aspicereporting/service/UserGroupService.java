package com.aspicereporting.service;

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

    public void addOrUpdateUserGroup(UserGroup userGroup) {
        userGroupRepository.save(userGroup);
    }

    public void deleteUserGroup(Long userGroupId) {
        Optional<UserGroup> userGroup = userGroupRepository.findById(userGroupId);

        //Delete only if group exists
        userGroup.ifPresentOrElse(
                (obj) -> {
                    //Handle many to many relation
                    for(User u : obj.getUsers()) {
                        u.setUserGroup(null);
                    }
                    userGroupRepository.delete(obj);
                },
                () -> {
                    throw new EntityNotFoundException("User group id " + userGroupId + " not found.");
                }
        );
    }

    public List<UserGroup> getAllUserGroupsList() {
        return userGroupRepository.findAll();
    }

    public void create(UserGroup group) {
        //Get group user ids
        List<Long> userIds = new ArrayList<>();
        if(!group.getUsers().isEmpty()) {
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
        for(User u : users) {
            u.setUserGroup(group);
        }

        userGroupRepository.save(group);
    }
}
