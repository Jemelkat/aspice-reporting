package com.aspicereporting.entity;

import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@JsonView(View.Simple.class)
@DiscriminatorValue("STATIC_TEXT")
@Entity
public class TextItem extends ReportItem {
    private String textArea;
}
