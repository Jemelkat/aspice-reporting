package com.aspicereporting.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.istack.NotNull;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = "username"), @UniqueConstraint(columnNames = "email")})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Size(max = 20)
    @NotNull
    private String username;

    @Size(max = 60)
    @NotNull
    private String email;

    @Size(max = 120)
    @NotNull
    @JsonIgnore
    private String password;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "group_id")
    @JsonIgnore
    private UserGroup userGroup;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonIgnore
    private Set<Source> sources = new HashSet<>();;

    @OneToMany(mappedBy="reportUser", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonIgnore
    private Set<Report> reports = new HashSet<>();

    @OneToMany(mappedBy="templateUser", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonIgnore
    private Set<Template> templates = new HashSet<>();

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public User(Long id, String username, String email, String password) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public User(Long id, String username, String email, String password, UserGroup userGroup) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.userGroup = userGroup;
    }

    public boolean isAdmin() {
        return roles
                .stream()
                .filter(r -> r.getName()
                        .equals(Role.ERole.ROLE_ADMIN))
                .findFirst()
                .isPresent();
    }
}