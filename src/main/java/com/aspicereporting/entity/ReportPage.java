package com.aspicereporting.entity;

import com.aspicereporting.entity.enums.Orientation;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@JsonView(View.Simple.class)
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "report_page")
public class ReportPage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "report_page_id")
    private Long id;

    @NotNull(message = "Report page needs orientation defined.")
    @Column(length = 20, name = "orientation",nullable = false)
    @Enumerated(EnumType.STRING)
    private Orientation orientation;

    @JsonView(View.Canvas.class)
    @OneToMany(mappedBy = "reportPage", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Valid
    private List<ReportItem> reportItems = new ArrayList<>();

    @JsonView(View.Simple.class)
    @ManyToOne
    @JoinColumn(name = "page_template_id")
    private Template pageTemplate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", referencedColumnName = "report_id")
    @JsonIgnore
    private Report report;

    public void addTemplate(Template template) {
        this.pageTemplate = template;
        template.getReportPages().add(this);
    }

    public void addReportItem(ReportItem item) {
        this.reportItems.add(item);
        item.setReportPage(this);
    }
}
