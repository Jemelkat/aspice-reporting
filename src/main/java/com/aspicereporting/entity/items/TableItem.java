package com.aspicereporting.entity.items;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@DiscriminatorValue("SIMPLE_TABLE")
@JsonView(View.Simple.class)
public class TableItem extends ReportItem {
    @ManyToOne
    private Source source;

    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @OrderColumn(name = "column_ordinal")
    private List<TableColumn> tableColumns = new ArrayList<>();
}
