package com.aspicereporting.entity.items;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@NoArgsConstructor
@Entity
@JsonView(View.Simple.class)
public class TableColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_column")
    private Long id;

    private int width;

    @ManyToOne
    @JoinColumn(name = "source_id", referencedColumnName = "source_id")
    @JsonIgnoreProperties({"sourceColumns", "sourceGroups"})
    private Source source;

    @ManyToOne
    @JoinColumn(name = "source_column_id", referencedColumnName = "source_column_id")
    private SourceColumn sourceColumn;

    @ManyToOne
    @JoinColumn(name = "capability_table_id", referencedColumnName = "report_item_id")
    @JsonIgnore
    private CapabilityTable capabilityTable;

    @ManyToOne
    @JoinColumn(name = "simple_table_id", referencedColumnName = "report_item_id")
    @JsonIgnore
    private TableItem simpleTable;

    public void addSource(Source source) {
        this.source = source;
        source.getTableColumns().add(this);
    }
    public void addSourceColumn(SourceColumn sourceColumn) {
        this.sourceColumn = sourceColumn;
        sourceColumn.getTableColumns().add(this);
    }
}
