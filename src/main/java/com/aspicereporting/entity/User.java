package com.aspicereporting.entity;

import com.aspicereporting.entity.items.*;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.sun.istack.NotNull;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.ArrayList;
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

    @Size(max = 20)
    @NotNull
    private String username;

    @Size(max = 60)
    @NotNull
    @JsonView(View.Detailed.class)
    private String email;

    @Size(max = 120)
    @NotNull
    @JsonIgnore
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
        //Remove used shared from this group
        for (Report report : reports) {
            for (ReportItem reportItem : report.getReportItems()) {
                switch (reportItem.getType()) {
                    case SIMPLE_TABLE:
                        SimpleTable simpleTable = (SimpleTable) reportItem;
                        if (simpleTable.getSource() != null) {
                            if (!this.getId().equals(simpleTable.getSource().getUser().getId())) {
                                if (!simpleTable.getSource().getSourceGroups().stream().anyMatch(group -> newGroups.contains(group.getId()))) {
                                    simpleTable.setSource(null);
                                    for (TableColumn tc : simpleTable.getTableColumns()) {
                                        tc.setSourceColumn(null);
                                    }
                                }
                            }
                        }
                        break;
                    case CAPABILITY_TABLE:
                        CapabilityTable capabilityTable = (CapabilityTable) reportItem;
                        if (capabilityTable.getSource() != null) {
                            if (!this.getId().equals(capabilityTable.getSource().getUser().getId())) {
                                if (!capabilityTable.getSource().getSourceGroups().stream().anyMatch(group -> newGroups.contains(group.getId()))) {
                                    capabilityTable.setSource(null);
                                    capabilityTable.setAssessorColumn(null);
                                    capabilityTable.getAssessorFilter().clear();
                                    capabilityTable.setProcessColumn(null);
                                    capabilityTable.getProcessFilter().clear();
                                    capabilityTable.setCriterionColumn(null);
                                    capabilityTable.setLevelColumn(null);
                                    capabilityTable.setScoreColumn(null);
                                }
                            }
                        }
                        break;
                    case LEVEL_PIE_GRAPH:
                        LevelPieGraph levelPieGraph = (LevelPieGraph) reportItem;
                        if (levelPieGraph.getSource() != null) {
                            if (!this.getId().equals(levelPieGraph.getSource().getUser().getId())) {
                                if (!levelPieGraph.getSource().getSourceGroups().stream().anyMatch(group -> newGroups.contains(group.getId()))) {
                                    levelPieGraph.setSource(null);
                                    levelPieGraph.setAssessorColumn(null);
                                    levelPieGraph.getAssessorFilter().clear();
                                    levelPieGraph.setProcessColumn(null);
                                    levelPieGraph.setCriterionColumn(null);
                                    levelPieGraph.setAttributeColumn(null);
                                    levelPieGraph.setScoreColumn(null);
                                }
                            }
                        }
                        break;
                    case LEVEL_BAR_GRAPH:
                        LevelBarGraph levelBarGraph = (LevelBarGraph) reportItem;
                        for (Source source : new ArrayList<>(levelBarGraph.getSources())) {
                            if (!this.getId().equals(source.getUser().getId())) {
                                if (!source.getSourceGroups().stream().anyMatch(group -> newGroups.contains(group.getId()))) {
                                    levelBarGraph.getSources().removeIf(s -> s.getId().equals(source.getId()));
                                    boolean assessor = false;
                                    boolean process = false;
                                    boolean attribute = false;
                                    boolean criterion = false;
                                    boolean score = false;
                                    for (Source remainingSources : levelBarGraph.getSources()) {
                                        for (SourceColumn sc : remainingSources.getSourceColumns()) {
                                            if (sc.getColumnName().equals(levelBarGraph.getAssessorColumnName())) {
                                                assessor = true;
                                            }
                                            if (sc.getColumnName().equals(levelBarGraph.getProcessColumnName())) {
                                                process = true;
                                            }
                                            if (sc.getColumnName().equals(levelBarGraph.getAttributeColumnName())) {
                                                attribute = true;
                                            }
                                            if (sc.getColumnName().equals(levelBarGraph.getCriterionColumnName())) {
                                                criterion = true;
                                            }
                                            if (sc.getColumnName().equals(levelBarGraph.getScoreColumnName())) {
                                                score = true;
                                            }
                                        }
                                    }
                                    if (!assessor) {
                                        levelBarGraph.setAssessorColumnName(null);
                                        levelBarGraph.getAssessorFilter().clear();
                                    }
                                    if (!process) {
                                        levelBarGraph.setProcessColumnName(null);
                                    }
                                    if (!criterion) {
                                        levelBarGraph.setCriterionColumnName(null);
                                    }
                                    if (!attribute) {
                                        levelBarGraph.setAttributeColumnName(null);
                                    }
                                    if (!score) {
                                        levelBarGraph.setScoreColumnName(null);
                                    }
                                }
                            }
                        }
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