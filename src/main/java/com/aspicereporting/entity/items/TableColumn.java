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
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Entity
@JsonView(View.Simple.class)
public class TableColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_column_id")
    private Long id;

    private int width;

    @NotNull(message = "Table requires all columns defined")
    @ManyToOne
    @JoinColumn(name = "source_column_id", referencedColumnName = "source_column_id")
    private SourceColumn sourceColumn;

    @ManyToOne
    @JoinColumn(name = "simple_table_id", referencedColumnName = "report_item_id")
    @JsonIgnore
    private TableItem simpleTable;
}
