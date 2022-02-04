package com.aspicereporting.entity.items;

import com.aspicereporting.entity.TextStyle;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.w3c.dom.Text;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@JsonView(View.Simple.class)
@DiscriminatorValue("TEXT")
@Entity
public class TextItem extends ReportItem {
    @Column(columnDefinition = "TEXT")
    private String textArea;

    @OneToOne(mappedBy = "textItem", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @JoinColumn(name = "style_id", referencedColumnName = "report_item_id")
    private TextStyle textStyle;
}
