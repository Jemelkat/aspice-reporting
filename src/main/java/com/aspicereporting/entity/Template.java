package com.aspicereporting.entity;

import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.*;

@Getter
@Setter
@JsonView(View.Simple.class)
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"template_name", "user_id"})})
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long id;

    @Column(length = 50, name = "template_name")
    @NotNull
    private String templateName;

    @JsonView(View.SimpleTable.class)
    @Column(name = "template_created")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date templateCreated;

    @JsonView(View.SimpleTable.class)
    @Column(name = "template_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date templateLastUpdated;

    @JsonView(View.SimpleTable.class)
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User templateUser;

    @JsonView(View.Canvas.class)
    @OneToMany(mappedBy = "template", cascade = {CascadeType.ALL}, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ReportItem> templateItems = new ArrayList<>();

    @OneToMany(mappedBy = "reportTemplate", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Report> reports = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "template_groups",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    private Set<UserGroup> templateGroups = new HashSet<>();


    public void addGroup(UserGroup group) {
        this.templateGroups.add(group);
        group.getTemplates().add(this);
    }

    public void removeGroup(UserGroup group) {
        this.templateGroups.remove(group);
        group.getTemplates().remove(this);
    }

    public void addItem(ReportItem reportItem) {
        if(!this.templateItems.contains(reportItem)) {
            this.templateItems.add(reportItem);
        }
        reportItem.setTemplate(this);
    }
}
