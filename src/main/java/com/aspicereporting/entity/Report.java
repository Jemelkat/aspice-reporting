package com.aspicereporting.entity;

import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;

@Getter
@Setter
@JsonView(View.Simple.class)
@Entity
@Table(name = "report", uniqueConstraints = {@UniqueConstraint(columnNames = {"report_name", "user_id"})})
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(length = 50, name = "report_name")
    @NotBlank(message = "Report name is required.")
    private String reportName;

    @Column(name = "report_created")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date reportCreated;

    @Column(name = "report_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date reportLastUpdated;

    @JsonView(View.Canvas.class)
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ReportItem> reportItems = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "template_id")
    private Template reportTemplate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "report_groups",
            joinColumns = @JoinColumn(name = "report_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    private Set<UserGroup> reportGroups = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User reportUser;

    public void addGroup(UserGroup group) {
        this.reportGroups.add(group);
        group.getReports().add(this);
    }

    public void removeGroup(UserGroup group) {
        this.reportGroups.remove(group);
        group.getReports().remove(this);
    }

    public void addTemplate(Template template) {
        this.reportTemplate = template;
        template.getReports().add(this);
    }
}
