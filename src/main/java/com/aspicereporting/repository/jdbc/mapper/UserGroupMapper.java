package com.aspicereporting.repository.jdbc.mapper;

import com.aspicereporting.entity.UserGroup;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserGroupMapper implements RowMapper<UserGroup> {
    @Override
    public UserGroup mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new UserGroup(rs.getLong("group_id"),
                rs.getString("group_name"));
    }

    public UserGroupMapper() {
    }
}
