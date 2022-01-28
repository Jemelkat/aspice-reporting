package com.aspicereporting.entity.items;

import com.aspicereporting.entity.TextStyle;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.w3c.dom.Text;

import javax.persistence.*;

@Getter
@Setter
@JsonView(View.Simple.class)
@DiscriminatorValue("STATIC_TEXT")
@Entity
public class TextItem extends ReportItem {
    @Column(columnDefinition = "TEXT")
    private String textArea;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "text_style", referencedColumnName = "style_id")
    private TextStyle textStyle;

    public void addTextStyle(TextStyle textStyle) {
        this.textStyle = textStyle;
        textStyle.setTextItem(this);
    }
}
