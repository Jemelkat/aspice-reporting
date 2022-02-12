package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.items.TableColumn;
import com.aspicereporting.entity.items.TableItem;
import com.aspicereporting.jasper.model.SimpleTableModel;
import net.sf.jasperreports.components.ComponentsExtensionsRegistryFactory;
import net.sf.jasperreports.components.table.DesignCell;
import net.sf.jasperreports.components.table.StandardColumn;
import net.sf.jasperreports.components.table.StandardTable;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.component.ComponentKey;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.engine.design.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SimpleTableService extends BaseTableService{
    public JRDesignComponentElement createElement(JasperDesign jasperDesign, TableItem tableItem, Integer tableCount, Map<String, Object> parameters) throws JRException {
        //Create data parameter for custom JRTableModelDataSource.class
        JRDesignParameter parameter = new JRDesignParameter();
        parameter.setValueClass(JRTableModelDataSource.class);
        parameter.setName(TABLE_DATA + tableCount);
        jasperDesign.addParameter(parameter);

        //Create table dataset
        JRDesignDataset tableSubdataset = new JRDesignDataset(false);
        tableSubdataset.setName(TABLE_DATASET + tableCount);

        //Creates required fields
        List<String> columnArrray = new ArrayList<>();
        int rows = 0;
        int columns = 0;
        for (TableColumn tableColumn : tableItem.getTableColumns()) {
            SourceColumn sourceColumn = tableColumn.getSourceColumn();

            JRDesignField field = new JRDesignField();
            field.setValueClass(String.class);
            field.setName(sourceColumn.getColumnName());
            tableSubdataset.addField(field);

            //Creates column names array
            columnArrray.add(sourceColumn.getColumnName());
            if (rows == 0 && columns == 0) {
                rows = sourceColumn.getSourceData().size();
                columns = tableItem.getTableColumns().size();
            }
        }
        jasperDesign.addDataset(tableSubdataset);

        //Creates object with data
        Object[][] test = new Object[rows][columns];
        for (int i = 0; i < tableItem.getTableColumns().size(); i++) {
            SourceColumn sc = tableItem.getTableColumns().get(i).getSourceColumn();
            for (int j = 0; j < sc.getSourceData().size(); j++) {
                test[j][i] = sc.getSourceData().get(j).getValue();
            }
        }

        //Creates data bean
        SimpleTableModel tableModel = new SimpleTableModel(rows, columns);
        tableModel.setColumnNames(columnArrray.toArray(new String[0]));
        tableModel.setData(test);

        //Adds data bean to parameters
        parameters.put(TABLE_DATA + tableCount, new JRTableModelDataSource(tableModel));


        //Create table element
        JRDesignComponentElement tableElement = createTableElement(jasperDesign, tableItem);
        StandardTable table = new StandardTable();
        //Define dataset for this table
        JRDesignDatasetRun datasetRun = new JRDesignDatasetRun();
        datasetRun.setDatasetName(TABLE_DATASET + tableCount);
        datasetRun.setDataSourceExpression(new JRDesignExpression(
                "$P{" + TABLE_DATA + tableCount + "}"));
        table.setDatasetRun(datasetRun);

        //Create all columns
        for (TableColumn column : tableItem.getTableColumns()) {
            String columnName = column.getSourceColumn().getColumnName();
            StandardColumn fieldColumn = createSimpleTableColumn(jasperDesign, column.getWidth(), 20, columnName, "$F{" + columnName + "}");
            table.addColumn(fieldColumn);
        }

        tableElement.setComponent(table);
        return tableElement;
    }
}