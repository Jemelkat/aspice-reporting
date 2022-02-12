package com.aspicereporting.entity.items;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.exception.InvalidDataException;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("CAPABILITY_BAR_GRAPH")
@JsonView(View.Simple.class)
public class CapabilityBarGraph extends ReportItem {

    @NotNull(message = "Capability bar graph needs orientation defined.")
    @Column(length = 20, name = "orientation",nullable = false)
    @Enumerated(EnumType.STRING)
    private Orientation orientation;

    @NotNull(message = "Capability bar graph needs source defined")
    @ManyToOne
    private Source source;

    @NotNull(message = "Capability bar graph needs process column defined")
    @ManyToOne
    @JoinColumn(name = "process_column_id", referencedColumnName = "source_column_id")
    private SourceColumn processColumn;

    @NotNull(message = "Capability bar graph needs level column defined")
    @ManyToOne
    @JoinColumn(name = "level_column_id", referencedColumnName = "source_column_id")
    private SourceColumn levelColumn;

    @NotNull(message = "Capability bar graph needs attribute column defined")
    @ManyToOne
    @JoinColumn(name = "attribute_column_id", referencedColumnName = "source_column_id")
    private SourceColumn attributeColumn;

    @NotNull(message = "Capability bar graph needs score/value column defined")
    @ManyToOne
    @JoinColumn(name = "score_column_id", referencedColumnName = "source_column_id")
    private SourceColumn scoreColumn;

    public void validate() {

        if (this.source.getId() == null) {
            throw new InvalidDataException("Capability bar graph has no source defined.");
        }
        if (this.processColumn.getId() == null) {
            throw new InvalidDataException("Capability bar graph has no process defined.");
        }
        if (this.levelColumn.getId() == null) {
            throw new InvalidDataException("Capability bar graph has no capability level column defined.");
        }
        if (this.attributeColumn.getId() == null) {
            throw new InvalidDataException("Capability bar graph has no capability level column defined.");
        }
        if (this.scoreColumn.getId() == null) {
            throw new InvalidDataException("Capability bar graph has no score column defined.");
        }
    }

    public enum Orientation {
        VERTICAL, HORIZONTAL
    }
}