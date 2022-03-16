package com.aspicereporting.entity.items;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.exception.InvalidDataException;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@DiscriminatorValue("SIMPLE_TABLE")
@JsonView(View.Simple.class)
public class SimpleTable extends ReportItem {
    @NotNull(message = "Simple table needs source defined")
    @ManyToOne
    private Source source;

    @NotEmpty(message = "Simple table needs at least one column defined.")
    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="simple_table_id", referencedColumnName = "report_item_id")
    @OrderColumn(name = "column_ordinal")
    private List<TableColumn> tableColumns = new ArrayList<>();

    public void validate() {
        if (this.source.getId() == null) {
            throw new InvalidDataException("Simple table has no source defined.");
        }
        for(TableColumn tableColumn : this.tableColumns) {
            if(tableColumn.getId() == null) {
                throw new InvalidDataException("Simple table needs all columns defined.");
            }
        }
    }
}
