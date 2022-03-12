package com.aspicereporting.entity.items;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.UserGroup;
import com.aspicereporting.entity.enums.Orientation;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("SOURCE_LEVEL_BAR_GRAPH")
@JsonView(View.Simple.class)
public class SourceLevelBarGraph extends ReportItem {
    @NotNull(message = "Sources level bar graph needs orientation defined.")
    @Column(length = 20, name = "orientation",nullable = false)
    @Enumerated(EnumType.STRING)
    private Orientation orientation;
    @NotEmpty(message = "Sources level bar graph needs assessor column defined.")
    private String assessorColumn;
    @NotEmpty(message = "Sources level bar graph needs process column defined.")
    private String processColumn;
    @NotEmpty(message = "Sources level bar graph needs attribute column defined.")
    private String attributeColumn;
    @NotEmpty(message = "Sources level bar graph needs score column defined.")
    private String scoreColumn;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "bar_graph_sources",
            joinColumns = @JoinColumn(name = "report_item_id"),
            inverseJoinColumns = @JoinColumn(name = "source_id"))
    private Set<Source> sources = new HashSet<>();

    public void validate() {

    }
}
