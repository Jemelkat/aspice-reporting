package com.aspicereporting.entity.items;
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
    @OneToMany(mappedBy = "simpleTable", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<TableColumn> tableColumns = new ArrayList<>();
}
