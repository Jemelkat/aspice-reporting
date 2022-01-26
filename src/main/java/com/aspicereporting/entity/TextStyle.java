package com.aspicereporting.entity;

import com.aspicereporting.entity.items.TextItem;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import javax.persistence.*;
import javax.print.attribute.TextSyntax;
import javax.validation.constraints.NotNull;

@Setter
@Getter
@JsonView(View.Simple.class)
@Entity
public class TextStyle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "style_id")
    private Long id;
    @Column(name = "font_size")
    private Integer fontSize;
    @Column(name = "bold")
    private boolean bold = false;
    @Column(name = "italic")
    private boolean italic = false;
    @Column(name = "underline")
    private boolean underline = false;
    @Column(length = 20, name = "color")
    private String color;
    @OneToOne(mappedBy = "textStyle")
    @JsonIgnore
    private TextItem textItem;

    public boolean isFilled() {
        return (fontSize != null && !fontSize.equals(11)) || bold || italic || underline || (color!= null && !color.isEmpty());
    }

    public boolean isSame(TextStyle otherStyle) {
        return fontSize.equals(otherStyle.fontSize) &&
                bold == otherStyle.bold &&
                italic == otherStyle.italic &&
                underline == otherStyle.underline &&
                color.equals(otherStyle.color);
    }
}
