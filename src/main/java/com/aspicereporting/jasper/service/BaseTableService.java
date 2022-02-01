package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.entity.items.TableItem;
import net.sf.jasperreports.components.ComponentsExtensionsRegistryFactory;
import net.sf.jasperreports.engine.component.ComponentKey;
import net.sf.jasperreports.engine.design.JRDesignComponentElement;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.PositionTypeEnum;

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
}
