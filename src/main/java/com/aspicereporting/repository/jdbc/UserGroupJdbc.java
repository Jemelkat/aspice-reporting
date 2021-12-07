package com.aspicereporting.repository.jdbc;

import com.aspicereporting.entity.User;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.repository.jdbc.dao.UserGroupDao;
import com.aspicereporting.repository.jdbc.mapper.UserGroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

public class UserGroupJdbc implements UserGroupDao{

    private JdbcTemplate jdbcTemplate;

    @Autowired
    UserJdbc userJdbc;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void init() {
        try (Statement stm = jdbcTemplate.getDataSource().getConnection().createStatement()) {
            stm.executeUpdate("CREATE TABLE user_group (" + "group_id INT NOT NULL," + " group_name varchar(255), " + " PRIMARY KEY (group_id)" + ");");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<UserGroup> getAll() {
        List<UserGroup> groups = jdbcTemplate.query("select * from user_group", new UserGroupMapper());
        return groups;
    }

    @Override
    public UserGroup findById(Long id) {
        try {
            return jdbcTemplate.queryForObject("select * from user_group where group_id=?", new Object[]{id}, new UserGroupMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void insert(UserGroup userGroup) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("insert into user_group (group_name) values (?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, userGroup.getGroupName());
            return ps;
        }, keyHolder);
        userGroup.setId((long) keyHolder.getKey());
    }

    @Override
    public void update(UserGroup userGroup) {
        //Update user group
        jdbcTemplate.update("UPDATE user_group SET group_name = ? WHERE group_id = ?",
                userGroup.getId(), userGroup.getClass());

        //Get all related users for this group
        List<User> users = userJdbc.findAllById(userGroup.getUsers()
                .stream()
                .map(User::getId)
                .collect(Collectors.toList()));

        //Change user group
        for(User u :  users) {
            u.setUserGroup(userGroup);
            userJdbc.update(u);
        }
    }
}
