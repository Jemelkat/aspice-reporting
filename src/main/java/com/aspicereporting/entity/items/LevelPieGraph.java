package com.aspicereporting.entity.items;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.exception.InvalidDataException;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("LEVEL_PIE_GRAPH")
@JsonView(View.Simple.class)
public class LevelPieGraph extends ReportItem{
    @NotNull(message = "Level pie graph needs source defined")
    @ManyToOne
    private Source source;

    @NotNull(message = "Level pie graph needs assessor column defined")
    @ManyToOne
    @JoinColumn(name = "assessor_column_id", referencedColumnName = "source_column_id")
    private SourceColumn assessorColumn;

    private String assessorFilter;

    @NotNull(message = "Level pie graph needs process column defined")
    @ManyToOne
    @JoinColumn(name = "process_column_id", referencedColumnName = "source_column_id")
    private SourceColumn processColumn;

    @NotNull(message = "Level pie graph needs level column defined")
    @ManyToOne
    @JoinColumn(name = "level_column_id", referencedColumnName = "source_column_id")
    private SourceColumn levelColumn;

    @NotNull(message = "Level pie graph needs attribute column defined")
    @ManyToOne
    @JoinColumn(name = "attribute_column_id", referencedColumnName = "source_column_id")
    private SourceColumn attributeColumn;

    @NotNull(message = "Level pie graph needs score/value column defined")
    @ManyToOne
    @JoinColumn(name = "score_column_id", referencedColumnName = "source_column_id")
    private SourceColumn scoreColumn;

    public void validate() {
        if (this.source.getId() == null) {
            throw new InvalidDataException("Level pie graph has no source defined.");
        }
        if (this.assessorColumn.getId() == null) {
            throw new InvalidDataException("Level pie graph has no assessor column defined.");
        }
        if (this.processColumn.getId() == null) {
            throw new InvalidDataException("Level pie graph has no process defined.");
        }
        if (this.levelColumn.getId() == null) {
            throw new InvalidDataException("Level pie graph has no capability level column defined.");
        }
        if (this.attributeColumn.getId() == null) {
            throw new InvalidDataException("Level pie graph has no capability level column defined.");
        }
        if (this.scoreColumn.getId() == null) {
            throw new InvalidDataException("Level pie graph has no score column defined.");
        }
    }
}
