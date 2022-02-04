package com.aspicereporting.entity;
import com.aspicereporting.entity.items.TextItem;
import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@JsonView(View.Simple.class)
@Setter
@Getter
@NoArgsConstructor
@Entity
public class TextStyle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "style_id")
    private Long id;
    @Column(name = "font_size")
    @NotNull(message = "Font size must be defined.")
    private Integer fontSize;
    @Column(name = "bold")
    private boolean bold = false;
    @Column(name = "italic")
    private boolean italic = false;
    @Column(name = "underline")
    private boolean underline = false;
    @Pattern(regexp = "^#(?:[0-9a-fA-F]{3}){1,2}$", message = "Font color needs to be in hex format.")
    @Column(name = "color")
    private String color;
    @OneToOne(optional=false)
    @JoinColumn(name ="report_item_id")
    @JsonIgnore
    private TextItem textItem;

    public boolean isFilled() {
        return (fontSize != null && !fontSize.equals(11)) || bold || italic || underline || (color!= null && !color.isEmpty() && !color.equals("#000000"));
    }

    public boolean isSame(TextStyle otherStyle) {
        return fontSize.equals(otherStyle.fontSize) &&
                bold == otherStyle.bold &&
                italic == otherStyle.italic &&
                underline == otherStyle.underline &&
                color.equals(otherStyle.color);
    }
}
