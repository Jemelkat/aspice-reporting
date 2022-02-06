package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.items.CapabilityTable;
import com.aspicereporting.jasper.model.SimpleTableModel;
import com.aspicereporting.repository.SourceColumnRepository;
import com.aspicereporting.repository.SourceRepository;
import com.aspicereporting.utils.NaturalOrderComparator;
import net.sf.jasperreports.components.table.DesignCell;
import net.sf.jasperreports.components.table.StandardColumn;
import net.sf.jasperreports.components.table.StandardColumnGroup;
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

    public JRDesignComponentElement createElement(JasperDesign jasperDesign, CapabilityTable capabilityTable, int tableCount, Map<String, Object> parameters) throws JRException {
        //Map for attributes for each level - {level : [a1,a2,a3]}
        Map<String, LinkedHashSet<String>> levelAttributesMap = new LinkedHashMap<>();

        //Create data parameter for custom JRTableModelDataSource.class
        JRDesignParameter parameter = new JRDesignParameter();
        parameter.setValueClass(JRTableModelDataSource.class);
        parameter.setName(TABLE_DATA + tableCount);
        jasperDesign.addParameter(parameter);

        //Create table dataset
        JRDesignDataset tableSubdataset = new JRDesignDataset(false);
        tableSubdataset.setName(TABLE_DATASET + tableCount);

        //Create table model with data
        SimpleTableModel tableModel = createTableModel(capabilityTable, levelAttributesMap);
        parameters.put(TABLE_DATA + tableCount, new JRTableModelDataSource(tableModel));

        //Add fields for columns
        JRDesignField processField = new JRDesignField();
        processField.setValueClass(String.class);
        processField.setName(columnArray.get(0));
        tableSubdataset.addField(processField);
        for (var key : levelAttributesMap.keySet()) {
            for (var column : levelAttributesMap.get(key)) {
                JRDesignField field = new JRDesignField();
                field.setValueClass(String.class);
                field.setName(column + key);
                tableSubdataset.addField(field);
            }
        }
        jasperDesign.addDataset(tableSubdataset);

        //Create table element
        JRDesignComponentElement tableElement = createTableElement(jasperDesign, capabilityTable);
        StandardTable table = new StandardTable();
        //Define dataset for this table
        JRDesignDatasetRun datasetRun = new JRDesignDatasetRun();
        datasetRun.setDatasetName(TABLE_DATASET + tableCount);
        datasetRun.setDataSourceExpression(new JRDesignExpression(
                "$P{" + TABLE_DATA + tableCount + "}"));
        table.setDatasetRun(datasetRun);

        float fontSize = capabilityTable.getFontSize();
        int rowHeight = (int) (1.8 * fontSize);
        //Add first process column
        StandardColumn processColumn = createCapabilityColumns(jasperDesign, capabilityTable.getProcessWidth(), rowHeight, fontSize, columnArray.get(0), columnArray.get(0), false);
        StandardColumnGroup processGroup = new StandardColumnGroup();
        processGroup.setWidth(processColumn.getWidth());
        DesignCell processHeader = new DesignCell();
        processHeader.setDefaultStyleProvider(jasperDesign);
        processHeader.setHeight(rowHeight);
        processGroup.setColumnHeader(processHeader);
        processGroup.addColumn(processColumn);
        table.addColumn(processGroup);

        //Create all columns
        for (String key : levelAttributesMap.keySet()) {
            List<StandardColumn> columnsList = new ArrayList<>();
            for (String columnName : levelAttributesMap.get(key)) {
                //Add new column
                columnsList.add(createCapabilityColumns(jasperDesign, capabilityTable.getCriterionWidth(), rowHeight, fontSize, columnName, columnName + key, true));
            }
            StandardColumnGroup columnGroup = new StandardColumnGroup();
            columnGroup.setWidth(columnsList.size() * capabilityTable.getCriterionWidth());
            DesignCell header = new DesignCell();
            header.setDefaultStyleProvider(jasperDesign);
            header.getLineBox().getPen().setLineWidth(1f);
            header.setHeight(rowHeight);


            JRDesignStaticText headerElement = new JRDesignStaticText(jasperDesign);
            headerElement.setX(0);
            headerElement.setY(0);
            headerElement.setWidth(columnsList.size() * capabilityTable.getCriterionWidth());
            headerElement.setHeight(rowHeight);
            headerElement.setText(key);
            headerElement.setFontSize(fontSize);
            headerElement.setFontName("DejaVu Serif");
            headerElement.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
            headerElement.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            header.addElement(headerElement);
            columnGroup.setColumnHeader(header);

            for (var column : columnsList) {
                columnGroup.addColumn(column);
            }
            table.addColumn(columnGroup);
        }

        tableElement.setComponent(table);
        return tableElement;
    }

    private SimpleTableModel createTableModel(CapabilityTable capabilityTable, Map<String, LinkedHashSet<String>> levelAttributesMap) {
        //Get all unique processes and levels
        List<String> processNames = sourceRepository.findDistinctByColumnId(capabilityTable.getProcessColumn().getSourceColumn().getId());
        List<String> levelNames = sourceRepository.findDistinctByColumnId(capabilityTable.getLevelColumn().getId());

        //Remove empty levels "" and processes ""
        levelNames = levelNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        processNames = processNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());

        //Sort alphabetically
        Collections.sort(levelNames, new NaturalOrderComparator());
        Collections.sort(processNames, new NaturalOrderComparator());
        //Get only first N levels - limited by paramater
        levelNames = levelNames.stream().limit(capabilityTable.getLevelLimit()).collect(Collectors.toList());

        //Get all data for process, level, attribute and score columns
        SourceColumn processColumn = sourceColumnRepository.findFirstById(capabilityTable.getProcessColumn().getSourceColumn().getId());
        SourceColumn levelColumn = sourceColumnRepository.findFirstById(capabilityTable.getLevelColumn().getId());
        SourceColumn attributeColumn = sourceColumnRepository.findFirstById(capabilityTable.getCriterionColumn().getId());
        SourceColumn scoreColumn = sourceColumnRepository.findFirstById(capabilityTable.getScoreColumn().getId());

        //MultiKey map to store value for each process, level and attribute combination - {(process, level, attribute) : value}
        MultiKeyMap valuesMap = new MultiKeyMap();

        //Get all possible attributes for level and store them in Map
        //TODO: make only one iteration - check if exist in hashmap and then add
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
            Collections.sort(attributesForLevel, new NaturalOrderComparator());
            levelAttributesMap.put(level, new LinkedHashSet<>(attributesForLevel));
        }

        //Get all column names
        columnArray.add("Process Name");
        for (String key : levelNames) {
            LinkedHashSet<String> criterionsList = levelAttributesMap.get(key);
            for (var criterion : criterionsList) {
                columnArray.add(criterion + key);
            }
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

            for (var levelKey : levelAttributesMap.keySet()) {
                for (var criterion : levelAttributesMap.get(levelKey)) {
                    String scoreValue = (String) valuesMap.get(new MultiKey(processName, levelKey, criterion));
                    //If process does not have this criterion measured
                    if (scoreValue == null) {
                        scoreValue = "";
                    }
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

    protected StandardColumn createCapabilityColumns(JasperDesign jasperDesign, int width, int height, float fontSize, String headerText, String detailExpression, boolean isStyled) throws JRException {
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
        headerElement.setFontSize(fontSize);
        headerElement.setFontName("DejaVu Serif");
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
        detailElement.setFontSize(fontSize);
        detailElement.setFontName("DejaVu Serif");
        detailElement.setExpression(new JRDesignExpression("$F{" + detailExpression + "}"));
        detailElement.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        detailElement.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);

        //Create capability table style
        if (isStyled) {
            JRDesignStyle jrDesignStyle = createCapabilityTableStyle(detailExpression);
            jasperDesign.addStyle(jrDesignStyle);
            detailElement.setStyle(jrDesignStyle);
        }

        detail.addElement(detailElement);
        column.setDetailCell(detail);

        return column;
    }

    private JRDesignStyle createCapabilityTableStyle(String expression) {
        JRDesignStyle jrDesignStyle = new JRDesignStyle();
        jrDesignStyle.setName(expression + "_" + tablesCounter + "_style");
        jrDesignStyle.setMode(ModeEnum.OPAQUE);

        //F - dark green
        JRDesignExpression FExpression = new JRDesignExpression();
        FExpression.setText("$F{" + expression + "}.equals(\"F\")");
        JRDesignConditionalStyle FCondStyle = new JRDesignConditionalStyle();
        FCondStyle.setConditionExpression(FExpression);
        FCondStyle.setBackcolor(new Color(0, 176, 80));
        jrDesignStyle.addConditionalStyle(FCondStyle);
        //L- green
        JRDesignExpression Lexpression = new JRDesignExpression();
        Lexpression.setText("$F{" + expression + "}.equals(\"L\")");
        JRDesignConditionalStyle LCondStyle = new JRDesignConditionalStyle();
        LCondStyle.setConditionExpression(Lexpression);
        LCondStyle.setBackcolor(new Color(255, 192, 0));
        jrDesignStyle.addConditionalStyle(LCondStyle);
        //P- orange
        JRDesignExpression Pexpression = new JRDesignExpression();
        Pexpression.setText("$F{" + expression + "}.equals(\"P\")");
        JRDesignConditionalStyle PCondStyle = new JRDesignConditionalStyle();
        PCondStyle.setConditionExpression(Pexpression);
        PCondStyle.setBackcolor(new Color(146, 208, 80));
        jrDesignStyle.addConditionalStyle(PCondStyle);
        //N- red
        JRDesignExpression Nexpression = new JRDesignExpression();
        Nexpression.setText("$F{" + expression + "}.equals(\"N\")");
        JRDesignConditionalStyle NCondStyle = new JRDesignConditionalStyle();
        NCondStyle.setConditionExpression(Nexpression);
        NCondStyle.setBackcolor(new Color(255, 0, 0));
        jrDesignStyle.addConditionalStyle(NCondStyle);
        return jrDesignStyle;
    }
}
