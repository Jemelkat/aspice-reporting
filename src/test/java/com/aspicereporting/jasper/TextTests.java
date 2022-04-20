package com.aspicereporting.jasper;

import com.aspicereporting.entity.items.TextItem;
import com.aspicereporting.entity.items.TextStyle;
import com.aspicereporting.jasper.service.TextService;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JasperDesign;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TextTests {
    private TextService textService;

    @BeforeEach
    public void beforeEach() {
        textService = new TextService();
    }

    @DisplayName("Create text element test")
    @Test
    public void createElementTest() throws JRException {
        JasperDesign jasperDesign = new JasperDesign();
        TextItem text = new TextItem();
        TextStyle style = new TextStyle();
        text.setTextStyle(style);
        text.setTextArea("Example text");
        JRDesignStaticText element = textService.createElement(jasperDesign, text, 0);

        Assertions.assertEquals("Example text", element.getText());
        Assertions.assertNotNull(element.getStyle());
        Assertions.assertEquals(1,jasperDesign.getStylesList().size());
    }
}
