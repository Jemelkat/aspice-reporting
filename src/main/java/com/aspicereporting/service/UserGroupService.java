package com.aspicereporting.service;

import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserGroupService {

    @Autowired
    UserGroupRepository userGroupRepository;

    public void addOrUpdateUserGroup(UserGroup userGroup) {
        userGroupRepository.save(userGroup);
    }

    public void deleteUserGroup(Long userGroupId) {
        Optional<UserGroup> userGroup = userGroupRepository.findById(userGroupId);

        userGroup.ifPresentOrElse(
                (obj) -> {
                    userGroupRepository.delete(obj);
                },
                () -> {
                    throw new EntityNotFoundException("User group id " + userGroupId + " not found.");
                }
        );
    }
}
