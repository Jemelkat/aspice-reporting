package com.aspicereporting.entity.items;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.Template;
import com.aspicereporting.entity.TextItem;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextItem.class, name = "STATIC_TEXT"),
        @JsonSubTypes.Type(value = TableItem.class, name = "TABLE"),
        @JsonSubTypes.Type(value = GraphItem.class, name = "GRAPH"),
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        discriminatorType = DiscriminatorType.STRING,
        name = "report_item_type"
)
@Getter
@Setter
@JsonView(View.Simple.class)
@Entity
@Table(name = "report_item")
public class ReportItem implements Comparable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_item_id")
    private long itemId;
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

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", referencedColumnName = "report_id")
    @JsonIgnore
    private Report report;

    public enum EItemType {
        STATIC_TEXT,GRAPH,TABLE,IMAGE;
    }

    @Override
    public int compareTo(Object o) {
        ReportItem i = (ReportItem)o;
        if (this.y >= i.y)
            return 1;
        else if (this.y < i.y)
            return -1;
        return 0;
    }

}
