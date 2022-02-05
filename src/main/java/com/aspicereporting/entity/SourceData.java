package com.aspicereporting.entity;

import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;


@JsonView(View.Simple.class)
@Setter
@Getter
@NoArgsConstructor
@Entity
public class SourceData {
    @Id
    @GeneratedValue
    @Column(name = "source_data_id")
    private Long id;

    @Column(name = "source_data_value")
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_column_id", nullable = false)
    @JsonIgnore
    private SourceColumn sourceColumn;

    public SourceData(String value) {
        this.value = value;
    }
}
