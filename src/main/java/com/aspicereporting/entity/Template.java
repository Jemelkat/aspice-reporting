package com.aspicereporting.entity;

import com.aspicereporting.entity.items.TemplateItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"template_name","user_id"})})
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(length = 50, name = "template_name")
    @NotNull
    private String templateName;

    @Column(name = "template_created")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date templateCreated;

    @Column(name = "template_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date templateLastUpdated;

    @OneToMany(mappedBy = "template",cascade = {CascadeType.ALL},orphanRemoval=true)
    private List<TemplateItem> templateItems = new ArrayList<>();

    @OneToMany(mappedBy = "reportTemplate", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Report> reports = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User templateUser;
}
