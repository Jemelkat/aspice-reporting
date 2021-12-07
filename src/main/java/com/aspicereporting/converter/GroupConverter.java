package com.aspicereporting.converter;

import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GroupConverter implements Converter<Long, UserGroup> {
    @Autowired
    UserGroupRepository userGroupRepository;

    @Override
    public UserGroup convert(Long id) {
        if (id == null) {
            return null;
        } else {
            Optional<UserGroup> userGroup = userGroupRepository.findById(id);
            if(userGroup.isPresent()){
                return userGroup.get();
            }
            else {
                return null;
            }
        }
    }
}
