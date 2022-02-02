package com.aspicereporting.entity.items;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
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
@DiscriminatorValue("CAPABILITY_TABLE")
@JsonView(View.Simple.class)
public class CapabilityTable extends ReportItem {
    @ManyToOne
    private Source source;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name="process_column_id")
    private TableColumn processColumn;
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="level_column_id")
    private SourceColumn levelColumn;
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="engineering_column_id")
    private SourceColumn engineeringColumn;
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="score_column_id")
    private SourceColumn scoreColumn;
}