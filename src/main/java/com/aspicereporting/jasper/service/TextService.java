package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.items.TextStyle;
import com.aspicereporting.entity.items.TextItem;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import org.springframework.stereotype.Service;

import java.awt.*;

@Service
public class TextService {
    /**
     * Creates JRDesignStaticText (level bar graph) which can be used in JasperDesign
     */
    public JRDesignStaticText createElement(JasperDesign jasperDesign, TextItem textItem, Integer styleCount) throws JRException {
        JRDesignStaticText jrStaticText = createStaticText(textItem);

        //Add style to text item - only if style is defined
        if (textItem.getTextStyle() != null) {
            JRDesignStyle textStyle = createTextStyle(textItem.getTextStyle(), styleCount++);
            jasperDesign.addStyle(textStyle);
            jrStaticText.setStyle(textStyle);
        }
        jrStaticText.setPositionType(PositionTypeEnum.FLOAT);
        return jrStaticText;
    }

    private JRDesignStaticText createStaticText(TextItem item) {
        JRDesignStaticText element = new JRDesignStaticText();
        element.setText(item.getTextArea());
        element.setX(item.getX());
        element.setY(item.getY());
        element.setWidth(item.getWidth());
        element.setHeight(item.getHeight());
        element.setPositionType(PositionTypeEnum.FLOAT);
        element.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
        return element;
    }

    public JRDesignStyle createTextStyle(TextStyle textStyle, int styleCount) {
        JRDesignStyle jrDesignStyle = new JRDesignStyle();
        jrDesignStyle.setName("Style" + styleCount);
        jrDesignStyle.setBold(Boolean.valueOf(textStyle.isBold()));
        jrDesignStyle.setItalic(Boolean.valueOf(textStyle.isItalic()));
        jrDesignStyle.setUnderline(Boolean.valueOf(textStyle.isUnderline()));
        jrDesignStyle.setForecolor(Color.decode(textStyle.getColor()));
        jrDesignStyle.setFontSize((float) textStyle.getFontSize());
        jrDesignStyle.setFontName("DejaVu Serif");
        return jrDesignStyle;
    }
}
