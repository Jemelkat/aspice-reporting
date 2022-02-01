package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.SourceData;
import com.aspicereporting.entity.items.CapabilityTable;
import com.aspicereporting.jasper.model.SimpleTableModel;
import com.aspicereporting.repository.SourceColumnRepository;
import com.aspicereporting.repository.SourceRepository;
import net.sf.jasperreports.components.table.DesignCell;
import net.sf.jasperreports.components.table.StandardColumn;
import net.sf.jasperreports.components.table.StandardTable;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CapabilityTableService extends BaseTableService {
    @Autowired
    SourceRepository sourceRepository;

    @Autowired
    SourceColumnRepository sourceColumnRepository;

    private List<String> columnArray = new ArrayList<>();
    private static int tablesCounter = 0;

    public JRDesignComponentElement createTableForJasperDesign(JasperDesign jasperDesign, CapabilityTable capabilityTable, int tableCount, Map<String, Object> parameters) throws JRException {
        //Create data parameter for custom JRTableModelDataSource.class
        JRDesignParameter parameter = new JRDesignParameter();
        parameter.setValueClass(JRTableModelDataSource.class);
        parameter.setName(TABLE_DATA + tableCount);
        jasperDesign.addParameter(parameter);

        //Create table dataset
        JRDesignDataset tableSubdataset = new JRDesignDataset(false);
        tableSubdataset.setName(TABLE_DATASET + tableCount);

        //Create table model
        SimpleTableModel tableModel = createTableModel(capabilityTable);
        parameters.put(TABLE_DATA + tableCount, new JRTableModelDataSource(tableModel));

        //Add fields for columns
        for (String column : columnArray) {
            JRDesignField field = new JRDesignField();
            field.setValueClass(String.class);
            field.setName(column);
            tableSubdataset.addField(field);
        }
        jasperDesign.addDataset(tableSubdataset);


        //Creates required fields
//        List<String> columnArrray = new ArrayList<>();
//        int rows = 0;
//        int columns = 0;
//        for (TableColumn tableColumn : tableItem.getTableColumns()) {
//            SourceColumn sourceColumn = tableColumn.getSourceColumn();
//

//
//            //Creates column names array
//            columnArrray.add(sourceColumn.getColumnName());
//            if (rows == 0 && columns == 0) {
//                rows = sourceColumn.getSourceData().size();
//                columns = tableItem.getTableColumns().size();
//            }
//        }

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
        StandardTable table = new StandardTable();
        //Define dataset for this table
        JRDesignDatasetRun datasetRun = new JRDesignDatasetRun();
        datasetRun.setDatasetName(TABLE_DATASET + tableCount);
        datasetRun.setDataSourceExpression(new JRDesignExpression(
                "$P{" + TABLE_DATA + tableCount + "}"));
        table.setDatasetRun(datasetRun);

        //Create all columns
        int index = 0;
        for (String columnName : columnArray) {
            int width = 25;
            if (index == 0) {
                index++;
                width = capabilityTable.getProcessColumn().getWidth();
            }
            StandardColumn fieldColumn = createCapabilityColumns(jasperDesign, width, 20, columnName, "$F{" + columnName + "}");
            table.addColumn(fieldColumn);
        }

        tableElement.setComponent(table);
        return tableElement;
    }

    private SimpleTableModel createTableModel(CapabilityTable capabilityTable) {
        //Get all unique processes and levels
        List<String> processNames = sourceRepository.findDistinctByColumnId(capabilityTable.getProcessColumn().getSourceColumn().getId());
        List<String> levelNames = sourceRepository.findDistinctByColumnId(capabilityTable.getLevelColumn().getId());

        //Remove empty levels "" and processes ""
        levelNames = levelNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        processNames = processNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());

        //Sort levels alphabetically
        Collections.sort(levelNames);

        //Get all data for process, level, attribute and score columns
        SourceColumn processColumn = sourceColumnRepository.findFirstById(capabilityTable.getProcessColumn().getSourceColumn().getId());
        SourceColumn levelColumn = sourceColumnRepository.findFirstById(capabilityTable.getLevelColumn().getId());
        SourceColumn attributeColumn = sourceColumnRepository.findFirstById(capabilityTable.getEngineeringColumn().getId());
        SourceColumn scoreColumn = sourceColumnRepository.findFirstById(capabilityTable.getScoreColumn().getId());

        //Map for attributes for each level - {level : [a1,a2,a3]}
        Map<String, LinkedHashSet<String>> levelAttributesMap = new HashMap<>();
        //MultiKey map to store value for each process, level and attribute combination - {(process, level, attribute) : value}
        MultiKeyMap valuesMap = new MultiKeyMap();

        //Get all possible attributes for level and store them in Map
        //TODO: make only one iteration - check if exist and then add
        for (String level : levelNames) {
            List<String> attributesForLevel = new ArrayList<>();

            //Get all attributes for this level
            for (int i = 0; i < attributeColumn.getSourceData().size(); i++) {
                String levelValue = levelColumn.getSourceData().get(i).getValue();
                String attributeValue = attributeColumn.getSourceData().get(i).getValue();

                if (levelValue.equals(level)) {
                    //Add attributes to map
                    attributesForLevel.add(attributeColumn.getSourceData().get(i).getValue());
                }

                //Create add value to multimap for process, level, attribute key
                String processValue = processColumn.getSourceData().get(i).getValue();
                String scoreValue = scoreColumn.getSourceData().get(i).getValue();
                MultiKey multiKey = new MultiKey(processValue, levelValue, attributeValue);
                valuesMap.put(multiKey, scoreValue);
            }
            //Sort attributes alphabetically
            Collections.sort(attributesForLevel);
            levelAttributesMap.put(level, new LinkedHashSet<>(attributesForLevel));
        }

        //Get all column names
        columnArray.add("Process Name");
        for (String key : levelNames) {
            columnArray.addAll(levelAttributesMap.get(key));
        }

        int rows = processNames.size();
        int columns = columnArray.size();
        //Creates object with data
        Object[][] test = new Object[rows][columns];

        int rowIndex = 0;
        for (String processName : processNames) {
            int columnIndex = 0;
            test[rowIndex][columnIndex] = processName;
            columnIndex++;

            for (String level : levelNames) {
                for (var x : levelAttributesMap.get(level)) {
                    String atribute = columnArray.get(columnIndex);
                    String scoreValue = (String) valuesMap.get(new MultiKey(processName, level, atribute));
                    test[rowIndex][columnIndex] = scoreValue;
                    columnIndex++;
                }
            }
            rowIndex++;
        }

        //Creates data bean
        SimpleTableModel tableModel = new SimpleTableModel(rows, columns);
        tableModel.setColumnNames(columnArray.toArray(new String[0]));
        tableModel.setData(test);
        tablesCounter++;
        return tableModel;
    }

    protected StandardColumn createCapabilityColumns(JasperDesign jasperDesign, int width, int height, String headerText, String detailExpression) throws JRException {
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
        headerElement.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);

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
        detailElement.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);

        //Create capability table style
        JRDesignStyle jrDesignStyle = createCapabilityTableStyle(headerText);
        jasperDesign.addStyle(jrDesignStyle);

        detailElement.setStyle(jrDesignStyle);
        detail.addElement(detailElement);
        column.setDetailCell(detail);

        return column;
    }

    private JRDesignStyle createCapabilityTableStyle(String columnName) {
        JRDesignStyle jrDesignStyle = new JRDesignStyle();
        jrDesignStyle.setName(columnName + "_"+ tablesCounter +"_style");
        jrDesignStyle.setFontSize((float) 10);
        jrDesignStyle.setFontName("DejaVu Serif");
        jrDesignStyle.setMode(ModeEnum.OPAQUE);

        //F - dark green
        JRDesignExpression FExpression = new JRDesignExpression();
        FExpression.setText("$F{" + columnName + "}.equals(\"F\")");
        JRDesignConditionalStyle FCondStyle = new JRDesignConditionalStyle();
        FCondStyle.setConditionExpression(FExpression);
        FCondStyle.setBackcolor(new Color(0, 176, 80));
        jrDesignStyle.addConditionalStyle(FCondStyle);
        //L- green
        JRDesignExpression Lexpression = new JRDesignExpression();
        Lexpression.setText("$F{" + columnName + "}.equals(\"L\")");
        JRDesignConditionalStyle LCondStyle = new JRDesignConditionalStyle();
        LCondStyle.setConditionExpression(Lexpression);
        LCondStyle.setBackcolor(new Color(255, 192, 0));
        jrDesignStyle.addConditionalStyle(LCondStyle);
        //P- orange
        JRDesignExpression Pexpression = new JRDesignExpression();
        Pexpression.setText("$F{" + columnName + "}.equals(\"P\")");
        JRDesignConditionalStyle PCondStyle = new JRDesignConditionalStyle();
        PCondStyle.setConditionExpression(Pexpression);
        PCondStyle.setBackcolor(new Color(146, 208, 80));
        jrDesignStyle.addConditionalStyle(PCondStyle);
        //N- red
        JRDesignExpression Nexpression = new JRDesignExpression();
        Nexpression.setText("$F{" + columnName + "}.equals(\"N\")");
        JRDesignConditionalStyle NCondStyle = new JRDesignConditionalStyle();
        NCondStyle.setConditionExpression(Nexpression);
        NCondStyle.setBackcolor(new Color(255, 0, 0));
        jrDesignStyle.addConditionalStyle(NCondStyle);
        return jrDesignStyle;
    }
}
