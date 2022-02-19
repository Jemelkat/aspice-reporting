package com.aspicereporting.entity.items;
import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.exception.InvalidDataException;
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
    private int fontSize;
    private int processWidth;
    private int criterionWidth;
    private int levelLimit;

    @NotNull(message = "Capability table needs source defined")
    @ManyToOne
    private Source source;

    @NotNull(message = "Capability table needs process column defined")
    @ManyToOne
    @JoinColumn(name="process_column_id")
    private SourceColumn processColumn;

    @NotNull(message = "Capability table needs level column defined")
    @ManyToOne
    @JoinColumn(name="level_column_id")
    private SourceColumn levelColumn;

    @NotNull(message = "Capability table needs criterion column defined")
    @ManyToOne
    @JoinColumn(name="criterion_column_id")
    private SourceColumn criterionColumn;

    @NotNull(message = "Capability table needs score column defined")
    @ManyToOne
    @JoinColumn(name="score_column_id")
    private SourceColumn scoreColumn;

    public void validate() {
        if (this.source.getId() == null) {
            throw new InvalidDataException("Capability table has no source defined.");
        }
        if (this.getProcessColumn() == null) {
            throw new InvalidDataException("Capability table has no process defined.");
        }
        if (this.levelColumn.getId() == null) {
            throw new InvalidDataException("Capability table has no capability level column defined.");
        }
        if (this.criterionColumn.getId() == null) {
            throw new InvalidDataException("Capability table has no criterion column defined.");
        }
        if (this.scoreColumn.getId() == null) {
            throw new InvalidDataException("Capability table has no score column defined.");
        }
    }
}