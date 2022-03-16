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
    private Integer specificLevel;

    @NotNull(message = "Capability table needs source defined")
    @ManyToOne
    private Source source;

    @NotNull(message = "Capability table needs assessor column defined")
    @ManyToOne
    @JoinColumn(name = "assessor_column_id", referencedColumnName = "source_column_id")
    private SourceColumn assessorColumn;

    private String assessorFilter;

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

    @NotNull(message = "Capability table needs score aggregate function defined.")
    @Column(length = 20, name = "score_function",nullable = false)
    @Enumerated(EnumType.STRING)
    private EFunction scoreFunction;

    public void validate() {
        if (this.source.getId() == null) {
            throw new InvalidDataException("Capability table has no source defined.");
        }
        if (this.assessorColumn.getId() == null) {
            throw new InvalidDataException("Capability table has no assessor column defined.");
        }
        if (this.getProcessColumn() == null) {
            throw new InvalidDataException("Capability table has no process column defined.");
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