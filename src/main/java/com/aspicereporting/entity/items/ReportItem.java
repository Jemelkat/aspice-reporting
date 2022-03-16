package com.aspicereporting.entity.items;

import com.aspicereporting.entity.Dashboard;
import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextItem.class, name = "TEXT"),
        @JsonSubTypes.Type(value = TableItem.class, name = "SIMPLE_TABLE"),
        @JsonSubTypes.Type(value = CapabilityTable.class, name = "CAPABILITY_TABLE"),
        @JsonSubTypes.Type(value = CapabilityBarGraph.class, name = "CAPABILITY_BAR_GRAPH"),
        @JsonSubTypes.Type(value = SourceLevelBarGraph.class, name = "SOURCE_LEVEL_BAR_GRAPH"),
        @JsonSubTypes.Type(value = LevelPieGraph.class, name = "LEVEL_PIE_GRAPH"),

})
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(
        discriminatorType = DiscriminatorType.STRING,
        name = "report_item_type"
)
@Getter
@Setter
@JsonView(View.Simple.class)
@Entity
@Table(name = "report_item")
public abstract class ReportItem implements Comparable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_item_id", unique = true)
    private Long id;
    @Column(length = 50, name = "report_item_type", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private EItemType type;
    @Column(name = "report_item_x")
    private int x;
    @Column(name = "report_item_y")
    private int y;
    @Column(name = "report_item_height")
    private int height;
    @Column(name = "report_item_width")
    private int width;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", referencedColumnName = "report_id")
    @JsonIgnore
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", referencedColumnName = "template_id")
    @JsonIgnore
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", referencedColumnName = "dashboard_id")
    @JsonIgnore
    private Dashboard dashboard;

    public enum EItemType {
        TEXT, SIMPLE_TABLE, CAPABILITY_TABLE, CAPABILITY_BAR_GRAPH, LEVEL_PIE_GRAPH, SOURCE_LEVEL_BAR_GRAPH;
    }

    public enum EFunction {
        MIN, MAX, AVG;
    }

    @Override
    public int compareTo(Object o) {
        ReportItem i = (ReportItem) o;
        if (this.y >= i.y)
            return 1;
        else if (this.y < i.y)
            return -1;
        return 0;
    }
}
