package com.aspicereporting.repository.jdbc.mapper;

import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserMapper implements RowMapper<User> {
    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new User(
                rs.getLong("st_user_id"),
                rs.getString("st_username"),
                rs.getString("st_email"),
                rs.getString("st_password"),
                new UserGroup(rs.getLong("stu_group_id"),  rs.getString("stu_group_name"))
        );
    }

    public UserMapper() {
    }
}
