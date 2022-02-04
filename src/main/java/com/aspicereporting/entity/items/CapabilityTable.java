package com.aspicereporting.entity.items;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Setter
@Getter
@Entity
@DiscriminatorValue("CAPABILITY_TABLE")
@JsonView(View.Simple.class)
public class CapabilityTable extends ReportItem {
    @NotNull(message = "Capability table needs source defined")
    @ManyToOne
    private Source source;

    @NotNull(message = "Capability table needs process column defined")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name="process_column_id")
    private TableColumn processColumn;

    @NotNull(message = "Capability table needs level column defined")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="level_column_id")
    private SourceColumn levelColumn;

    @NotNull(message = "Capability table needs criterion column defined")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="engineering_column_id")
    private SourceColumn engineeringColumn;

    @NotNull(message = "Capability table needs score column defined")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="score_column_id")
    private SourceColumn scoreColumn;
}