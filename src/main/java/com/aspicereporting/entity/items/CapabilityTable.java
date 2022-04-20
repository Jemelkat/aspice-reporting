package com.aspicereporting.entity.items;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.enums.ScoreFunction;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.exception.InvalidDataException;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@DiscriminatorValue("CAPABILITY_TABLE")
@JsonView(View.Simple.class)
public class CapabilityTable extends ReportItem {
    @Min(value = 1, message = "Capability table font size must be bigger than 0.")
    private Integer fontSize = 10;
    @Min(value = 1, message = "Capability table process width must be bigger than 0.")
    private int processWidth = 100;
    @Min(value = 1, message = "Capability table criterion width must be bigger than 0.")
    private int criterionWidth = 25;
    @Min(value = 1, message = "Capability table level limit must be bigger than 0.")
    @Max(value = 5, message = "Capability table level limit max is 5.")
    private int levelLimit = 5;
    @Min(value = 1, message = "Capability table specific level must be bigger than 0.")
    @Max(value = 5, message = "Capability table specific level max is 5.")
    private Integer specificLevel;

    @NotNull(message = "Capability table needs source defined")
    @ManyToOne
    private Source source;

    @NotNull(message = "Capability table needs assessor column defined")
    @ManyToOne
    @JoinColumn(name = "assessor_column_id", referencedColumnName = "source_column_id")
    private SourceColumn assessorColumn;
    @Type(type = "list-array")
    @Column(
            name = "assessor_filter",
            columnDefinition = "text[]"
    )
    private List<String> assessorFilter = new ArrayList<>();

    @NotNull(message = "Capability table needs process column defined")
    @ManyToOne
    @JoinColumn(name = "process_column_id")
    private SourceColumn processColumn;
    @Type(type = "list-array")
    @Column(
            name = "process_filter",
            columnDefinition = "text[]"
    )
    private List<String> processFilter = new ArrayList<>();
    @NotNull(message = "Capability table needs level column defined")
    @ManyToOne
    @JoinColumn(name = "level_column_id")
    private SourceColumn levelColumn;

    @NotNull(message = "Capability table needs criterion column defined")
    @ManyToOne
    @JoinColumn(name = "criterion_column_id")
    private SourceColumn criterionColumn;

    @NotNull(message = "Capability table needs score column defined")
    @ManyToOne
    @JoinColumn(name = "score_column_id")
    private SourceColumn scoreColumn;
    @NotNull(message = "Capability table needs score aggregate function defined.")
    @Column(length = 20, name = "aggregate_scores", nullable = false)
    @Enumerated(EnumType.STRING)
    private ScoreFunction aggregateScoresFunction = ScoreFunction.MAX;

    public void validate() {
        if (this.source == null || this.source.getId() == null) {
            throw new InvalidDataException("Capability table has no source defined.");
        }
        if (this.assessorColumn == null || this.assessorColumn.getId() == null) {
            throw new InvalidDataException("Capability table has no assessor column defined.");
        }
        if (this.processColumn == null || this.getProcessColumn().getId() == null) {
            throw new InvalidDataException("Capability table has no process column defined.");
        }
        if (this.levelColumn == null || this.levelColumn.getId() == null) {
            throw new InvalidDataException("Capability table has no capability level column defined.");
        }
        if (this.criterionColumn == null || this.criterionColumn.getId() == null) {
            throw new InvalidDataException("Capability table has no criterion column defined.");
        }
        if (this.scoreColumn == null || this.scoreColumn.getId() == null) {
            throw new InvalidDataException("Capability table has no score column defined.");
        }
    }

    public void userGroupRemove(User user, List<Long> newGroups) {
        if (this.getSource() != null) {
            if (!user.getId().equals(this.getSource().getUser().getId())) {
                if (!this.getSource().getSourceGroups().stream().anyMatch(group -> newGroups.contains(group.getId()))) {
                    this.setSource(null);
                    this.setAssessorColumn(null);
                    this.getAssessorFilter().clear();
                    this.setProcessColumn(null);
                    this.getProcessFilter().clear();
                    this.setCriterionColumn(null);
                    this.setLevelColumn(null);
                    this.setScoreColumn(null);
                }
            }
        }
    }
}