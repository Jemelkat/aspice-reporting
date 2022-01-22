package com.aspicereporting.service;

import com.aspicereporting.entity.Group;
import com.aspicereporting.entity.User;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.repository.UserGroupRepository;
import com.aspicereporting.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
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
        Group userGroup = userGroupRepository.getUserGroupById(groupId);
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
                throw new EntityNotFoundException("Could not find user with id " + userId);
            }
        }

        userGroup.addUser(user);
        userRepository.save(user);
    }

    public void createOrUpdate(User user) {
        userRepository.save(user);
    }

    public void update(User user) {
        //If ID is not defined we cannot update the user
        if(user.getId() == null) {
            throw new EntityNotFoundException("Cannot update user without ID.");
        }

        //Get user from DB for updates
        User currentUser = null;
        try {
            currentUser = userRepository.findById(user.getId()).get();
        }catch (NoSuchElementException e) {
            throw new EntityNotFoundException("Could not find user with id " + user.getId(), e);
        }

        //Set new unsername and email
        currentUser.setUsername(user.getUsername());
        currentUser.setEmail(user.getEmail());

        userRepository.save(currentUser);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void delete(Long userId) {
        userRepository.findById(userId).ifPresentOrElse(
                (obj) -> {
                    userRepository.delete(obj);
                },
                () -> {
                    throw new EntityNotFoundException("User id " + userId + " not found.");
                }
        );
    }

    public void removeUserFromGroup(User user, Group userGroup) {
        //TODO Change tempalte, source and reports shared group
    }

    public void addUserToGroup(User user, Group userGroup) {
        //TODO Change tempalte, source and reports shared group
    }

    public List<Group> getAllGroups(User user) {
        return userGroupRepository.findAllByUsersContains(user);
    }
}
