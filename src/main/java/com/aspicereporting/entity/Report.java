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
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;

@JsonView(View.Simple.class)
@Getter
@Setter
@NoArgsConstructor
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

    @JsonView(View.Table.class)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @Column(name = "report_created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reportCreated;

    @JsonView(View.Table.class)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @Column(name = "report_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reportLastUpdated;


    @JsonView(View.Canvas.class)
    @OneToMany(mappedBy = "report", cascade = {CascadeType.ALL},orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderColumn(name = "pages_ordinal")
    private List<ReportPage> reportPages = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User reportUser;
}
