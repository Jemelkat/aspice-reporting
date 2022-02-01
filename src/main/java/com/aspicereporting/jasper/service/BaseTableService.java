package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.items.TableItem;
import net.sf.jasperreports.components.ComponentsExtensionsRegistryFactory;
import net.sf.jasperreports.components.table.DesignCell;
import net.sf.jasperreports.components.table.StandardColumn;
import net.sf.jasperreports.components.table.StandardGroupCell;
import net.sf.jasperreports.engine.component.ComponentKey;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;

import java.awt.*;

public class BaseTableService {
    protected static final String TABLE_DATA = "tableData";
    protected static final String TABLE_DATASET = "tableDataset";

    protected JRDesignComponentElement createTableElement(JasperDesign jasperDesign, ReportItem tableItem){

        JRDesignComponentElement tableElement = new JRDesignComponentElement(jasperDesign);
        tableElement.setX(tableItem.getX());
        tableElement.setY(tableItem.getY());
        tableElement.setWidth(tableItem.getWidth());
        tableElement.setHeight(tableItem.getHeight());
        tableElement.setPositionType(PositionTypeEnum.FLOAT);

        ComponentKey componentKey = new ComponentKey(ComponentsExtensionsRegistryFactory.NAMESPACE, "c",
                ComponentsExtensionsRegistryFactory.TABLE_COMPONENT_NAME);
        tableElement.setComponentKey(componentKey);
        return tableElement;
    }

    protected StandardColumn createSimpleTableColumn(JasperDesign jasperDesign, int width, int height, String headerText, String detailExpression) {
        StandardColumn column = new StandardColumn();
        column.setWidth(width);

        //column header
        DesignCell header = new DesignCell();
        header.setDefaultStyleProvider(jasperDesign);
        header.getLineBox().getPen().setLineWidth(1f);
        header.setHeight(height);

        JRDesignStaticText headerElement = new JRDesignStaticText(jasperDesign);
        headerElement.setX(0);
        headerElement.setY(0);
        headerElement.setWidth(width);
        headerElement.setHeight(height);
        headerElement.setText(headerText);
        headerElement.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);

        header.addElement(headerElement);
        column.setColumnHeader(header);

        //column detail
        DesignCell detail = new DesignCell();
        detail.setDefaultStyleProvider(jasperDesign);
        detail.getLineBox().getPen().setLineWidth(1f);
        detail.setHeight(height);

        JRDesignTextField detailElement = new JRDesignTextField(jasperDesign);
        detailElement.setX(0);
        detailElement.setY(0);
        detailElement.setWidth(width);
        detailElement.setHeight(height);
        detailElement.setExpression(new JRDesignExpression(detailExpression));
        detailElement.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);


        JRDesignExpression jrDesignExpression = new JRDesignExpression();
        jrDesignExpression.setText("new String($F{total}).equalsIgnoreCase(F)");
        JRDesignConditionalStyle jrDesignConditionalStyle = new JRDesignConditionalStyle();
        jrDesignConditionalStyle.setConditionExpression(jrDesignExpression);


        detail.addElement(detailElement);
        column.setDetailCell(detail);

        return column;
    }

}
