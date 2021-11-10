package com.aspicereporting.service;

import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.repository.UserGroupRepository;
import com.aspicereporting.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserGroupRepository userGroupRepository;

    @Transactional
    public void addGroupToUser(Long groupId, Long userId, User user) {
        //Get userGroupFromDb
        UserGroup userGroup = userGroupRepository.getUserGroupById(groupId);
        if (userGroup == null) {
            throw new EntityNotFoundException("Could not find user group.");
        }

        //Use existing user or logger user
        if(userId != null) {
            Optional<User> foundUser = userRepository.findById(userId);
            if(foundUser.isPresent()) {
                user = foundUser.get();
            }
            else {
                throw new EntityNotFoundException("Cound not find user with id " + userId);
            }
        }

        userGroup.addUser(user);
        userRepository.save(user);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
