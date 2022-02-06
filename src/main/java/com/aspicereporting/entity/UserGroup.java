package com.aspicereporting.entity;

import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.*;

@JsonView(View.Simple.class)
@Setter
@Getter
@NoArgsConstructor
@Entity
public class UserGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;
    @Column(name = "group_name", unique = true)
    @NotNull
    private String groupName;

    @JsonView(View.Simple.class)
    @ManyToMany(mappedBy = "userGroups", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    @JsonView(View.Detailed.class)
    @ManyToMany(mappedBy = "sourceGroups", fetch = FetchType.LAZY)
    private Set<Source> sources = new HashSet<>();

    public UserGroup(Long id, String groupName) {
        this.id = id;
        this.groupName = groupName;
    }

    public void addUser(User user) {
        user.addUserGroup(this);
        this.users.add(user);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof UserGroup)) return false;

        return id != null && id.equals(((UserGroup) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
