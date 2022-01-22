package com.aspicereporting.entity;

import com.aspicereporting.entity.items.TemplateItem;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@JsonView(View.Simple.class)
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"template_name", "user_id"})})
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(length = 50, name = "template_name")
    @NotNull
    private String templateName;

    @JsonView(View.SimpleTable.class)
    @Column(name = "template_created")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date templateCreated;

    @JsonView(View.SimpleTable.class)
    @Column(name = "template_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date templateLastUpdated;

    @ManyToOne
    @JoinColumn(name = "group_id")
    @JsonIgnore
    private Group templateGroup;

    @JsonView(View.SimpleTable.class)
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User templateUser;

    @JsonView(View.Canvas.class)
    @OneToMany(mappedBy = "template", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<TemplateItem> templateItems = new ArrayList<>();

    @OneToMany(mappedBy = "reportTemplate", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Report> reports = new ArrayList<>();

    public void removeReport(Report report) {
        this.reports.remove(report);
        report.setReportTemplate(null);
    }
}
