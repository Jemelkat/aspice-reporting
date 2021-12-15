package com.aspicereporting.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Entity
public class UserGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;
    @Column(name = "group_name", unique = true)
    @NotNull
    private String groupName;

    @OneToMany (mappedBy = "userGroup", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @OneToMany (mappedBy = "templateGroup", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Template> templates  = new ArrayList<>();

    @OneToMany (mappedBy = "reportGroup", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Report> reports  = new ArrayList<>();

    public UserGroup(Long id, String groupName) {
        this.id = id;
        this.groupName = groupName;
    }

    public void addUser(User user) {
        user.setUserGroup(this);
        this.users.add(user);
    }
}
