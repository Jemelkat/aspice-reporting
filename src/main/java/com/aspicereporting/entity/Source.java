package com.aspicereporting.entity;

import com.aspicereporting.entity.items.*;
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
    @ManyToMany(mappedBy = "sources", fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private Set<LevelBarGraph> levelBarGraphs = new HashSet<>();

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
            table.getAssessorFilter().clear();
            table.setProcessColumn(null);
            table.getProcessFilter().clear();
            table.setCriterionColumn(null);
            table.setLevelColumn(null);
            table.setScoreColumn(null);
        }
        for (LevelBarGraph graph : levelBarGraphs) {
            graph.getSources().removeIf(source -> source.getId().equals(this.id));
        }
        for (LevelPieGraph graph : levelPieGraphs) {
            graph.setSource(null);
            graph.setAssessorColumn(null);
            graph.getAssessorFilter().clear();
            graph.setProcessColumn(null);
            graph.setCriterionColumn(null);
            graph.setAttributeColumn(null);
            graph.setScoreColumn(null);
        }
        this.simpleSimpleTables.clear();
        this.levelPieGraphs.clear();
        this.capabilityTables.clear();
        this.levelBarGraphs.clear();
    }

    public void addGroup(UserGroup group) {
        this.sourceGroups.add(group);
        group.getSources().add(this);
    }

    public void removeGroup(UserGroup group) {
        this.sourceGroups.remove(group);
        group.getSources().remove(this);
    }

    public void removeFromItemsOnUnshare(User owner, List<Long> newGroupsIds) {
        for (SimpleTable simpleTable : simpleSimpleTables) {
            User user = simpleTable.findItemOwner();
            if (!owner.getId().equals(user.getId())) {
                if (!user.getUserGroups().stream().anyMatch(group -> newGroupsIds.contains(group.getId()))) {
                    simpleTable.setSource(null);
                    for (TableColumn tc : simpleTable.getTableColumns()) {
                        tc.setSourceColumn(null);
                    }
                }
            }
        }
        for (CapabilityTable capabilityTable : capabilityTables) {
            User user = capabilityTable.findItemOwner();
            if (!owner.getId().equals(user.getId())) {
                if (!user.getUserGroups().stream().anyMatch(group -> newGroupsIds.contains(group.getId()))) {
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
        for (LevelPieGraph graph : levelPieGraphs) {
            User user = graph.findItemOwner();
            if (!owner.getId().equals(user.getId())) {
                if (!user.getUserGroups().stream().anyMatch(group -> newGroupsIds.contains(group.getId()))) {
                    graph.setSource(null);
                    graph.setAssessorColumn(null);
                    graph.getAssessorFilter().clear();
                    graph.setProcessColumn(null);
                    graph.setCriterionColumn(null);
                    graph.setAttributeColumn(null);
                    graph.setScoreColumn(null);
                }
            }
        }
        for (LevelBarGraph graph : levelBarGraphs) {
            User user = graph.findItemOwner();
            if (!owner.getId().equals(user.getId())) {
                if (!user.getUserGroups().stream().anyMatch(group -> newGroupsIds.contains(group.getId()))) {
                    graph.getSources().removeIf(source -> source.getId().equals(this.id));
                    boolean assessor = false;
                    boolean process = false;
                    boolean attribute = false;
                    boolean criterion = false;
                    boolean score = false;
                    for (Source remainingSources : graph.getSources()) {
                        for (SourceColumn sc : remainingSources.getSourceColumns()) {
                            if(sc.getColumnName().equals(graph.getAssessorColumnName())) {
                                assessor = true;
                            }
                            if(sc.getColumnName().equals(graph.getProcessColumnName())) {
                                process = true;
                            }
                            if(sc.getColumnName().equals(graph.getAttributeColumnName())) {
                                attribute = true;
                            }
                            if(sc.getColumnName().equals(graph.getCriterionColumnName())) {
                                criterion = true;
                            }
                            if(sc.getColumnName().equals(graph.getScoreColumnName())) {
                                score = true;
                            }
                        }
                    }
                    if(!assessor) {
                        graph.setAssessorColumnName(null);
                        graph.getAssessorFilter().clear();
                    }
                    if(!process) {
                        graph.setProcessColumnName(null);
                    }
                    if(!criterion) {
                        graph.setCriterionColumnName(null);
                    }
                    if(!attribute) {
                        graph.setAttributeColumnName(null);
                    }
                    if(!score) {
                        graph.setScoreColumnName(null);
                    }
                }
            }
        }
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
