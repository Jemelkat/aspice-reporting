package com.aspicereporting.entity;

import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonView(View.Simple.class)
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"source_name", "user_id"})})
public class Source {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "source_created")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sourceCreated;

    @Column(name = "source_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sourceLastUpdated;

    @JsonView(View.Detailed.class)
    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SourceColumn> sourceColumns;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "source_groups",
            joinColumns = @JoinColumn(name = "source_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    private Set<UserGroup> sourceGroups = new HashSet<>();

    public void removeFromAllGroups() {
        for(UserGroup group : sourceGroups) {
            group.getSources().remove(this);
        }
        this.sourceGroups.clear();
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
