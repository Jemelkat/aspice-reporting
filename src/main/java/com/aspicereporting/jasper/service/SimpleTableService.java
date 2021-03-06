package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.items.SimpleTable;
import com.aspicereporting.entity.items.TableColumn;
import com.aspicereporting.jasper.model.SimpleTableModel;
import net.sf.jasperreports.components.table.StandardColumn;
import net.sf.jasperreports.components.table.StandardTable;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.engine.design.*;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SimpleTableService extends BaseTableService {
    /**
     * Creates JRDesignComponentElement (table) which can be used in JasperDesign
     */
    public JRDesignComponentElement createElement(JasperDesign jasperDesign, SimpleTable simpleTableItem, Integer tableCount, Map<String, Object> parameters) throws JRException {
        //Create data parameter for custom JRTableModelDataSource.class
        JRDesignParameter parameter = new JRDesignParameter();
        parameter.setValueClass(JRTableModelDataSource.class);
        parameter.setName(TABLE_DATA + tableCount);
        jasperDesign.addParameter(parameter);

        //Create table dataset
        JRDesignDataset tableSubdataset = new JRDesignDataset(false);
        tableSubdataset.setName(TABLE_DATASET + tableCount);

        //Creates required fields
        List<String> columnArray = new ArrayList<>();
        int rows = 0;
        int columns = 0;
        int columnCount = 0;
        for (TableColumn tableColumn : simpleTableItem.getTableColumns()) {
            SourceColumn sourceColumn = tableColumn.getSourceColumn();

            JRDesignField field = new JRDesignField();
            field.setValueClass(String.class);
            field.setName(sourceColumn.getColumnName() + columnCount);
            tableSubdataset.addField(field);
            columnCount++;
            //Creates column names array
            columnArray.add(sourceColumn.getColumnName());
            if (rows == 0 && columns == 0) {
                rows = sourceColumn.getSourceData().size();
                columns = simpleTableItem.getTableColumns().size();
            }
        }
        jasperDesign.addDataset(tableSubdataset);


        //Creates object with data
        Object[][] dataObject;
        if (rows == 0) {
            dataObject = new Object[1][columns];
            for (int i = 0; i < columns; i++) {
                dataObject[0][i] = "";
            }
        } else {
            dataObject = new Object[rows][columns];
            for (int i = 0; i < simpleTableItem.getTableColumns().size(); i++) {
                SourceColumn sc = simpleTableItem.getTableColumns().get(i).getSourceColumn();
                for (int j = 0; j < sc.getSourceData().size(); j++) {
                    dataObject[j][i] = sc.getSourceData().get(j).getValue();
                }
            }
        }

        //Creates data bean
        SimpleTableModel tableModel = new SimpleTableModel(rows, columns);
        String[] columnNamesUpdated = new String[columnArray.size()];
        for (int i = 0; i < columnArray.size(); i++) {
            columnNamesUpdated[i] = columnArray.get(i) + i;
        }
        tableModel.setColumnNames(columnNamesUpdated);
        tableModel.setData(dataObject);
        //Adds data bean to parameters
        parameters.put(TABLE_DATA + tableCount, new JRTableModelDataSource(tableModel));


        //Create table element
        JRDesignComponentElement tableElement = createTableElement(jasperDesign, simpleTableItem);
        StandardTable table = new StandardTable();
        //Define dataset for this table
        JRDesignDatasetRun datasetRun = new JRDesignDatasetRun();
        datasetRun.setDatasetName(TABLE_DATASET + tableCount);
        datasetRun.setDataSourceExpression(new JRDesignExpression(
                "$P{" + TABLE_DATA + tableCount + "}"));
        table.setDatasetRun(datasetRun);

        //Create all columns
        columnCount = 0;
        for (TableColumn column : simpleTableItem.getTableColumns()) {
            String columnName = column.getSourceColumn().getColumnName();
            StandardColumn fieldColumn = createSimpleTableColumn(jasperDesign, column.getWidth(), 20, columnName, "$F{" + columnName + columnCount + "}");
            table.addColumn(fieldColumn);
            columnCount++;
        }

        tableElement.setComponent(table);
        return tableElement;
    }
}
