package com.aspicereporting.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Entity
public class UserGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;
    @Column(name = "group_name")
    @NotNull
    private String groupName;
    @OneToMany (mappedBy = "userGroup")
    private List<User> users;

    public void addUser(User user) {
        user.setUserGroup(this);
        this.users.add(user);
    }
}
