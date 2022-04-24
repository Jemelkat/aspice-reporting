package com.aspicereporting.entity;

import com.aspicereporting.entity.items.*;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonView(View.Simple.class)
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

    @Column(length = 20)
    private String username;

    @JsonView(View.Detailed.class)
    @Column(length = 60)
    private String email;

    @JsonIgnore
    @Column(length = 120)
    private String password;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonView(View.Detailed.class)
    private Set<Role> roles = new HashSet<>();

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_groups",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    private Set<UserGroup> userGroups = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Source> sources = new HashSet<>();
    ;

    @JsonIgnore
    @OneToMany(mappedBy = "reportUser", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Report> reports = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "templateUser", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Template> templates = new HashSet<>();

    @JsonIgnore
    @OneToOne(mappedBy = "dashboardUser", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private Dashboard dashboard;

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

    public void addUserGroup(UserGroup userGroup) {
        this.userGroups.add(userGroup);
        userGroup.getUsers().add(this);
    }

    public void removeUserGroup(UserGroup userGroup) {
        this.userGroups.remove(userGroup);
        userGroup.getUsers().remove(this);
        //Remove my shared sources with this group
        List<Long> newGroups = userGroups.stream().map(group -> group.getId()).collect(Collectors.toList());
        for (Source source : sources) {
            source.removeGroup(userGroup);
            source.removeFromItemsOnUnshare(this, source.getSourceGroups().stream().map(group -> group.getId()).collect(Collectors.toList()));
        }
        //Remove used shared sources in items from this group
        for (Report report : reports) {
            for (ReportPage reportPage : report.getReportPages()) {
                for (ReportItem reportItem : reportPage.getReportItems()) {
                    switch (reportItem.getType()) {
                        case SIMPLE_TABLE:
                            ((SimpleTable) reportItem).userGroupRemove(this, newGroups);
                            break;
                        case CAPABILITY_TABLE:
                            ((CapabilityTable) reportItem).userGroupRemove(this, newGroups);
                            break;
                        case LEVEL_PIE_GRAPH:
                            ((LevelPieGraph) reportItem).userGroupRemove(this, newGroups);
                            break;
                        case LEVEL_BAR_GRAPH:
                            ((LevelBarGraph) reportItem).userGroupRemove(this, newGroups);
                            break;
                    }
                }
            }
        }
        for (Template template : templates) {
            for (ReportItem templateItem : template.getTemplateItems()) {
                switch (templateItem.getType()) {
                    case SIMPLE_TABLE:
                        ((SimpleTable) templateItem).userGroupRemove(this, newGroups);
                        break;
                    case CAPABILITY_TABLE:
                        ((CapabilityTable) templateItem).userGroupRemove(this, newGroups);
                        break;
                    case LEVEL_PIE_GRAPH:
                        ((LevelPieGraph) templateItem).userGroupRemove(this, newGroups);
                        break;
                    case LEVEL_BAR_GRAPH:
                        ((LevelBarGraph) templateItem).userGroupRemove(this, newGroups);
                        break;
                }
            }
        }
        if (dashboard != null) {
            for (ReportItem dashboardItem : dashboard.getDashboardItems()) {
                switch (dashboardItem.getType()) {
                    case LEVEL_PIE_GRAPH:
                        ((LevelPieGraph) dashboardItem).userGroupRemove(this, newGroups);
                        break;
                    case LEVEL_BAR_GRAPH:
                        ((LevelBarGraph) dashboardItem).userGroupRemove(this, newGroups);
                        break;
                }
            }
        }

    }

    @JsonIgnore
    public boolean isAdmin() {
        return roles
                .stream()
                .filter(r -> r.getName()
                        .equals(Role.ERole.ROLE_ADMIN))
                .findFirst()
                .isPresent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof User)) return false;

        return id != null && id.equals(((User) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}