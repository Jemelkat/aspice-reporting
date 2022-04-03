package com.aspicereporting.entity;

import com.aspicereporting.entity.enums.Orientation;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonView(View.Simple.class)
@Getter
@Setter
@NoArgsConstructor
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

    @NotNull(message = "Template needs orientation defined.")
    @Column(length = 20, name = "orientation",nullable = false)
    @Enumerated(EnumType.STRING)
    private Orientation orientation;

    @JsonView(View.Table.class)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @Column(name = "template_created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date templateCreated;

    @JsonView(View.Table.class)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @Column(name = "template_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date templateLastUpdated;

    @JsonView(View.Table.class)
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User templateUser;

    @JsonView(View.Canvas.class)
    @OneToMany(mappedBy = "template", cascade = {CascadeType.ALL}, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ReportItem> templateItems = new ArrayList<>();

    @OneToMany(mappedBy = "pageTemplate", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ReportPage> reportPages = new ArrayList<>();
}
