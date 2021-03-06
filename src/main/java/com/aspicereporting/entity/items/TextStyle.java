package com.aspicereporting.entity.items;

import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Min;
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
    @NotNull(message = "Font size must be defined.")
    @Min(value = 1, message = "Font size must be bigger than 0.")
    @Column(name = "font_size")
    private Integer fontSize = 11;
    @Column(name = "bold")
    private boolean bold = false;
    @Column(name = "italic")
    private boolean italic = false;
    @Column(name = "underline")
    private boolean underline = false;
    @Pattern(regexp = "^#(?:[0-9a-fA-F]{3}){1,2}$", message = "Font color needs to be in hex format.")
    @Column(name = "color")
    private String color = "#000000";

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    private TextItem textItem;

    public boolean isSame(TextStyle otherStyle) {
        return fontSize.equals(otherStyle.fontSize) &&
                bold == otherStyle.bold &&
                italic == otherStyle.italic &&
                underline == otherStyle.underline &&
                color.equals(otherStyle.color);
    }
}
