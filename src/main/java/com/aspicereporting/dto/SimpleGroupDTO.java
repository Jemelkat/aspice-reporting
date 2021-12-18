package com.aspicereporting.dto;

import com.aspicereporting.entity.UserGroup;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"users"})
public class SimpleGroupDTO extends UserGroup {
    private SimpleGroupDTO(UserGroup userGroup) {
        setId(userGroup.getId());
        setGroupName(userGroup.getGroupName());
    }

    public static SimpleGroupDTO getInstance(UserGroup userGroup) {
        if(userGroup == null) {
            return null;
        }
        return new SimpleGroupDTO(userGroup);
    }
}

