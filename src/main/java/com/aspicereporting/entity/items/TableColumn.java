package com.aspicereporting.entity.items;

import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

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

    @Min(value = 1, message = "Capability table criterion width must be bigger than 0.")
    private int width = 50;

    @NotNull(message = "Table requires all columns defined")
    @ManyToOne
    @JoinColumn(name = "source_column_id", referencedColumnName = "source_column_id")
    private SourceColumn sourceColumn;
}
