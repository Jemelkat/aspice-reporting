package com.aspicereporting.entity.items;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.enums.Orientation;
import com.aspicereporting.entity.enums.ScoreFunction;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonView;
import com.vladmihalcea.hibernate.type.array.ListArrayType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("LEVEL_BAR_GRAPH")
@JsonView(View.Simple.class)
@TypeDef(
        name = "list-array",
        typeClass = ListArrayType.class
)
public class LevelBarGraph extends ReportItem {
    @NotNull(message = "Level bar graph needs orientation defined.")
    @Column(length = 20, name = "orientation",nullable = false)
    @Enumerated(EnumType.STRING)
    private Orientation orientation;
    @NotEmpty(message = "Level bar graph needs assessor column defined.")
    private String assessorColumnName;
    @Type(type = "list-array")
    @Column(
            name = "assessor_filter",
            columnDefinition = "text[]"
    )
    private List<String> assessorFilter = new ArrayList<>();
    @NotEmpty(message = "Level bar graph needs process column defined.")
    private String processColumnName;
    @Type(type = "list-array")
    @Column(
            name = "process_filter",
            columnDefinition = "text[]"
    )
    private List<String> processFilter = new ArrayList<>();
    @NotEmpty(message = "Level bar graph needs attribute column defined.")
    private String attributeColumnName;
    @NotEmpty(message = "Level bar graph needs criterion column defined.")
    private String criterionColumnName;
    @NotEmpty(message = "Level bar graph needs score column defined.")
    private String scoreColumnName;
    @NotNull(message = "Level bar graph scores aggregate function cannot be null.")
    @Column(length = 20, name = "aggregate_scores",nullable = false)
    @Enumerated(EnumType.STRING)
    private ScoreFunction aggregateScoresFunction;
    @NotNull(message = "Level bar graph aggregate levels cannot be null. Please use true/false.")
    @Column(name = "aggregate_levels")
    private boolean aggregateLevels = false;
    @NotNull(message = "Level bar graph sources aggregate function cannot be null.")
    @Column(length = 20, name = "aggregate_sources",nullable = false)
    @Enumerated(EnumType.STRING)
    private ScoreFunction aggregateSourcesFunction;

    @NotEmpty(message = "Level bar graph needs sources.")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "bar_graph_sources",
            joinColumns = @JoinColumn(name = "report_item_id"),
            inverseJoinColumns = @JoinColumn(name = "source_id"))
    @OrderColumn(name="graph_source_order")
    private List<Source> sources = new ArrayList<>();

    public void validate() {

    }

    public void userGroupRemove(User user, List<Long> newGroups) {
        for (Source source : new ArrayList<>(this.getSources())) {
            if (!user.getId().equals(source.getUser().getId())) {
                if (!source.getSourceGroups().stream().anyMatch(group -> newGroups.contains(group.getId()))) {
                    this.getSources().removeIf(s -> s.getId().equals(source.getId()));
                    boolean assessor = false;
                    boolean process = false;
                    boolean attribute = false;
                    boolean criterion = false;
                    boolean score = false;
                    for (Source remainingSources : this.getSources()) {
                        for (SourceColumn sc : remainingSources.getSourceColumns()) {
                            if (sc.getColumnName().equals(this.getAssessorColumnName())) {
                                assessor = true;
                            }
                            if (sc.getColumnName().equals(this.getProcessColumnName())) {
                                process = true;
                            }
                            if (sc.getColumnName().equals(this.getAttributeColumnName())) {
                                attribute = true;
                            }
                            if (sc.getColumnName().equals(this.getCriterionColumnName())) {
                                criterion = true;
                            }
                            if (sc.getColumnName().equals(this.getScoreColumnName())) {
                                score = true;
                            }
                        }
                    }
                    if (!assessor) {
                        this.setAssessorColumnName(null);
                        this.getAssessorFilter().clear();
                    }
                    if (!process) {
                        this.setProcessColumnName(null);
                    }
                    if (!criterion) {
                        this.setCriterionColumnName(null);
                    }
                    if (!attribute) {
                        this.setAttributeColumnName(null);
                    }
                    if (!score) {
                        this.setScoreColumnName(null);
                    }
                }
            }
        }
    }
}
