package com.aspicereporting.repository.jdbc;

import com.aspicereporting.entity.User;
import com.aspicereporting.repository.jdbc.dao.UserDao;
import com.aspicereporting.repository.jdbc.mapper.UserGroupMapper;
import com.aspicereporting.repository.jdbc.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

public class UserJdbc implements UserDao {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try (Statement stm = jdbcTemplate.getDataSource().getConnection().createStatement()) {
            stm.executeUpdate("CREATE TABLE users ("
                    + "user_id INT NOT NULL,"
                    + "username varchar(255),"
                    + "group_id INT REFERENCES user_group (group_id), "
                    + " PRIMARY KEY (user_id)"
                    +");");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(User user) {
        jdbcTemplate.update("UPDATE users SET username = ?, email = ?, group_id = ? WHERE group_id = ?",
                user.getUsername(), user.getEmail(), user.getUserGroup().getId());
    }

    @Override
    public User findById(Long id) {
        try {
            return jdbcTemplate.queryForObject("select st.*, ug.*  from users join user_group ug on ug.group_id = st.user_id where user_id =?", new Object[]{id}, new UserMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<User> findAllById(List<Long> id) {
        String inSql = String.join(",", Collections.nCopies(id.size(), "?"));
        List<User> users = jdbcTemplate.query(String.format("select * from users where id IN (%s)",inSql), id.toArray(), new UserMapper());
        return users;
    }
}
