package com.aspicereporting.entity;

import com.aspicereporting.entity.items.TableColumn;
import com.aspicereporting.entity.items.TableItem;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;


@JsonView(View.Simple.class)
@Setter
@Getter
@NoArgsConstructor
@Entity
public class SourceColumn {
    @Id
    @GeneratedValue
    @Column(name = "source_column_id")
    private Long id;

    @Column(name = "column_name")
    private String columnName;

    @JsonView(View.Detailed.class)
    @OneToMany(mappedBy = "sourceColumn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderColumn(name = "column_ordinal")
    private List<SourceData> sourceData = new ArrayList<>();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    public SourceColumn(String columnName) {
        this.columnName = columnName;
    }

    public void addSourceData(SourceData sourceData) {
        sourceData.setSourceColumn(this);
        this.sourceData.add(sourceData);
    }
}
