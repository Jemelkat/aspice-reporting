package com.aspicereporting.entity;

import com.aspicereporting.entity.items.TableColumn;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.*;

@JsonView(View.Simple.class)
@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"source_name", "user_id"})})
public class Source {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_id")
    private Long id;

    @Column(name = "source_name")
    private String sourceName;

    @JsonView(View.SimpleTable.class)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @Column(name = "source_created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sourceCreated;

    @JsonView(View.SimpleTable.class)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @Column(name = "source_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sourceLastUpdated;

    @JsonView(View.Detailed.class)
    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SourceColumn> sourceColumns;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "source_groups", joinColumns = @JoinColumn(name = "source_id"), inverseJoinColumns = @JoinColumn(name = "group_id"))
    private Set<UserGroup> sourceGroups = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "source", fetch = FetchType.LAZY)
    private List<TableColumn> tableColumns = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    public void removeFromAllGroups() {
        for (UserGroup group : sourceGroups) {
            group.getSources().remove(this);
        }
        this.sourceGroups.clear();
    }

    public void removeFromAllTableColumns() {
        for(TableColumn tableColumn : tableColumns) {
            tableColumn.setSourceColumn(null);
            tableColumn.setSource(null);
        }
        this.tableColumns.clear();
    }

    public void addGroup(UserGroup group) {
        this.sourceGroups.add(group);
        group.getSources().add(this);
    }

    public void removeGroup(UserGroup group) {
        this.sourceGroups.remove(group);
        group.getSources().remove(this);
    }
}
