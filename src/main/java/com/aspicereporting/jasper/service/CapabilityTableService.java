package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.items.CapabilityTable;
import com.aspicereporting.repository.SourceColumnRepository;
import com.aspicereporting.repository.SourceRepository;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.engine.design.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CapabilityTableService extends BaseTableService {
    @Autowired
    SourceRepository sourceRepository;

    @Autowired
    SourceColumnRepository sourceColumnRepository;

    public JRDesignComponentElement createTableForJasperDesign(JasperDesign jasperDesign, CapabilityTable capabilityTable, int tableCount, Map<String, Object> parameters) throws JRException {
        //Create data parameter for custom JRTableModelDataSource.class
        JRDesignParameter parameter = new JRDesignParameter();
        parameter.setValueClass(JRTableModelDataSource.class);
        parameter.setName(TABLE_DATA + tableCount);
        jasperDesign.addParameter(parameter);

        //Create table dataset
        JRDesignDataset tableSubdataset = new JRDesignDataset(false);
        tableSubdataset.setName(TABLE_DATASET + tableCount);


        createTableModel(capabilityTable);
        //Creates required fields
//        List<String> columnArrray = new ArrayList<>();
//        int rows = 0;
//        int columns = 0;
//        for (TableColumn tableColumn : tableItem.getTableColumns()) {
//            SourceColumn sourceColumn = tableColumn.getSourceColumn();
//
//            JRDesignField field = new JRDesignField();
//            field.setValueClass(String.class);
//            field.setName(sourceColumn.getColumnName());
//            tableSubdataset.addField(field);
//
//            //Creates column names array
//            columnArrray.add(sourceColumn.getColumnName());
//            if (rows == 0 && columns == 0) {
//                rows = sourceColumn.getSourceData().size();
//                columns = tableItem.getTableColumns().size();
//            }
//        }
//        jasperDesign.addDataset(tableSubdataset);
//
//        //Creates object with data
//        Object[][] test = new Object[rows][columns];
//        for (int i = 0; i < tableItem.getTableColumns().size(); i++) {
//            SourceColumn sc = tableItem.getTableColumns().get(i).getSourceColumn();
//            for (int j = 0; j < sc.getSourceData().size(); j++) {
//                test[j][i] = sc.getSourceData().get(j).getValue();
//            }
//        }
//
//        //Creates data bean
//        SimpleTableModel tableModel = new SimpleTableModel(rows, columns);
//        tableModel.setColumnNames(columnArrray.toArray(new String[0]));
//        tableModel.setData(test);
//
//        parameters.put(TABLE_DATA + tableCount, new JRTableModelDataSource(tableModel));
//
//
//        //Create table element
        JRDesignComponentElement tableElement = createTableElement(jasperDesign, capabilityTable);
//        StandardTable table = new StandardTable();
//        //Define dataset for this table
//        JRDesignDatasetRun datasetRun = new JRDesignDatasetRun();
//        datasetRun.setDatasetName(TABLE_DATASET + tableCount);
//        datasetRun.setDataSourceExpression(new JRDesignExpression(
//                "$P{" + TABLE_DATA + tableCount + "}"));
//        table.setDatasetRun(datasetRun);
//
//        //Create all columns
//        for (TableColumn column : tableItem.getTableColumns()) {
//            String columnName = column.getSourceColumn().getColumnName();
//            StandardColumn fieldColumn = createSimpleTableColumn(jasperDesign, column.getWidth(), 20, columnName, "$F{" + columnName + "}");
//            table.addColumn(fieldColumn);
//        }
//
//        tableElement.setComponent(table);
        return tableElement;
    }

    private void createTableModel(CapabilityTable capabilityTable) {
        List<String> processNames = sourceRepository.findDistinctByColumnId(capabilityTable.getProcessColumn().getSourceColumn().getId());
        List<String> levelNames = sourceRepository.findDistinctByColumnId(capabilityTable.getLevelColumn().getId());
        List<String> criteriumNames = sourceRepository.findDistinctByColumnId(capabilityTable.getEngineeringColumn().getId());

        Collections.sort(levelNames);

        List<Long> ids = new ArrayList<>();
        ids.add(capabilityTable.getProcessColumn().getSourceColumn().getId());
        ids.add(capabilityTable.getLevelColumn().getId());
        ids.add(capabilityTable.getEngineeringColumn().getId());
        ids.add(capabilityTable.getSource().getId());
        List<SourceColumn> sourceColumns = sourceColumnRepository.findAllByIdIn(ids);
        SourceColumn process = sourceColumns.get(0);
        SourceColumn atribute = sourceColumns.get(3);
        System.out.println("test");

        for (int i = 0; i < sourceColumns.get(0).getSourceData().size(); i++) {

        }
    }
}
