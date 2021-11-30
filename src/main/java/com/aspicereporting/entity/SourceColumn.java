package com.aspicereporting.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SourceColumn {
    @Id
    @GeneratedValue
    @Column(name = "source_column_id")
    private Long id;

    @Column(name = "column_name")
    private String columnName;

    @OneToMany (mappedBy = "sourceColumn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SourceData> sourceData = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "source_id", nullable = false)
    @JsonIgnore
    private Source source;

    public SourceColumn(String columnName) {
        this.columnName = columnName;
    }

    public void addSourceData(SourceData sourceData){
        sourceData.setSourceColumn(this);
        this.sourceData.add(sourceData);
    }
}
