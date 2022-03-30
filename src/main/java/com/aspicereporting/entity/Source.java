package com.aspicereporting.entity;

import com.aspicereporting.entity.items.*;
import com.aspicereporting.entity.items.SimpleTable;
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
@javax.persistence.Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"source_name", "user_id"})})
public class Source {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_id")
    private Long id;

    @Column(name = "source_name")
    private String sourceName;

    @JsonView(View.Table.class)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @Column(name = "source_created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sourceCreated;

    @JsonView(View.Table.class)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @Column(name = "source_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sourceLastUpdated;

    @JsonView(View.Detailed.class)
    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderColumn(name = "column_ordinal")
    private List<SourceColumn> sourceColumns = new ArrayList<>();

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(name = "source_groups", joinColumns = @JoinColumn(name = "source_id"), inverseJoinColumns = @JoinColumn(name = "group_id"))
    private Set<UserGroup> sourceGroups = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "source", fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private List<CapabilityTable> capabilityTables = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "source", fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private List<SimpleTable> simpleSimpleTables = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "source", fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private List<LevelPieGraph> levelPieGraphs = new ArrayList<>();

    @JsonIgnore
    @ManyToMany(mappedBy = "sources", fetch = FetchType.LAZY)
    private Set<SourceLevelBarGraph> sourceLevelBarGraphs = new HashSet<>();

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    public void prepareForDelete() {
        for (SimpleTable simpleTable : simpleSimpleTables) {
            simpleTable.setSource(null);
            for (TableColumn tc : simpleTable.getTableColumns()) {
                tc.setSourceColumn(null);
            }
        }
        for (CapabilityTable table : capabilityTables) {
            table.setSource(null);
            table.setAssessorColumn(null);
            table.setProcessColumn(null);
            table.setCriterionColumn(null);
            table.setLevelColumn(null);
            table.setScoreColumn(null);
        }
        for (SourceLevelBarGraph graph : sourceLevelBarGraphs) {
            graph.getSources().removeIf(source -> source.getId() == this.id);
        }
        for (LevelPieGraph graph : levelPieGraphs) {
            graph.setSource(null);
            graph.setAssessorColumn(null);
            graph.setProcessColumn(null);
            graph.setCriterionColumn(null);
            graph.setAttributeColumn(null);
            graph.setScoreColumn(null);
        }
        this.simpleSimpleTables.clear();
        this.levelPieGraphs.clear();
        this.capabilityTables.clear();
        this.sourceLevelBarGraphs.clear();
    }

    public void addGroup(UserGroup group) {
        this.sourceGroups.add(group);
        group.getSources().add(this);
    }

    public void removeGroup(UserGroup group) {
        this.sourceGroups.remove(group);
        group.getSources().remove(this);
    }

    public void addSourceColumns(List<SourceColumn> columns) {
        for (var column : columns) {
            this.sourceColumns.add(column);
            column.setSource(this);
        }
    }

    public void addCapabilityTable(CapabilityTable capabilityTable) {
        this.capabilityTables.add(capabilityTable);
        capabilityTable.setSource(this);
    }

    public void addSimpleTable(SimpleTable simpleTable) {
        this.simpleSimpleTables.add(simpleTable);
        simpleTable.setSource(this);
    }

    public void addLevelPieGraph(LevelPieGraph levelPieGraph) {
        this.levelPieGraphs.add(levelPieGraph);
        levelPieGraph.setSource(this);
    }



}
