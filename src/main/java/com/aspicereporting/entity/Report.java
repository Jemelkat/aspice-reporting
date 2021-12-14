package com.aspicereporting.entity;

import com.aspicereporting.entity.items.ReportItem;
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
@Table(name = "report", uniqueConstraints = {@UniqueConstraint(columnNames = {"report_name","user_id"})})
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(length = 50, name = "report_name")
    @NotNull
    private String reportName;

    @Column(name = "report_created")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date reportCreated;

    @Column(name = "report_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date reportLastUpdated;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ReportItem> reportItems = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name="template_id")
    private Template reportTemplate;

    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    @JsonIgnore
    private User reportUser;
}
