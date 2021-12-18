package com.aspicereporting.dto;

import com.aspicereporting.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"email", "roles"})
public class SimpleUserDTO extends User {
    private SimpleUserDTO(User user) {
        setId(user.getId());
        setUsername(user.getUsername());
    }

    public static SimpleUserDTO getInstance(User user) {
        if(user == null) {
            return null;
        }
        return new SimpleUserDTO(user);
    }
}
