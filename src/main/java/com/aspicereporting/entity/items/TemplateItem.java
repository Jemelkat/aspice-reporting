package com.aspicereporting.entity.items;

import com.aspicereporting.entity.Template;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
public class TemplateItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_item_id")
    private Long itemId;
    @Column(length = 50, name = "template_item_type")
    @Enumerated(EnumType.STRING)
    private ReportItem.EItemType type;
    @Column(name = "template_item_x")
    private int x;
    @Column(name = "template_item_y")
    private int y;
    @Column(name = "template_item_height")
    private int height;
    @Column(name = "template_item_width")
    private int width;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", referencedColumnName = "template_id")
    @JsonIgnore
    private Template template;

    public void update(TemplateItem newTemplateItem) {
        this.x = newTemplateItem.getX();
        this.y = newTemplateItem.getY();
        this.height = newTemplateItem.getHeight();
        this.width = newTemplateItem.getWidth();
        this.type = newTemplateItem.getType();
    }
}
