package com.aspicereporting.entity;

import com.aspicereporting.entity.items.ReportItem;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@DiscriminatorValue("STATIC_TEXT")
@Entity
public class TextItem extends ReportItem {
    private String textArea;
}
