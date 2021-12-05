package com.aspicereporting.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
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

    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    @JsonIgnore
    private User reportUser;

    @OneToMany(mappedBy = "report", cascade = {CascadeType.ALL},orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReportItem> reportItems = new ArrayList<>();
}
