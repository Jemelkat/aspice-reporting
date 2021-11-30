package com.aspicereporting.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SourceData {
    @Id
    @GeneratedValue
    @Column(name = "source_data_id")
    private Long id;

    @Column(name = "source_data_value")
    private String value;

    @ManyToOne
    @JoinColumn(name = "source_column_id", nullable = false)
    @JsonIgnore
    private SourceColumn sourceColumn;

    public SourceData(String value) {
        this.value = value;
    }
}
