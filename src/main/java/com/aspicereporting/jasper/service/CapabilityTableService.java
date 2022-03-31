package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.items.CapabilityTable;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.exception.JasperReportException;
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
                field.setName(column);
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
                columnsList.add(createCapabilityColumns(jasperDesign, capabilityTable.getCriterionWidth(), rowHeight, fontSize, columnName, columnName, true));
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
        //Get all unique processes, levels and assessors
        List<String> processNames = sourceRepository.findDistinctColumnValuesForColumn(capabilityTable.getProcessColumn().getId());
        List<String> levelNames = sourceRepository.findDistinctColumnValuesForColumn(capabilityTable.getLevelColumn().getId());
        List<String> assessorNames = sourceRepository.findDistinctColumnValuesForColumn(capabilityTable.getAssessorColumn().getId());

        //Remove empty ""
        levelNames = levelNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        processNames = processNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        assessorNames = assessorNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());

        //Apply assessor filter = if no assessor is defined - we will use first assessor found
        if (capabilityTable.getAssessorFilter() != null && !capabilityTable.getAssessorFilter().equals("")) {
            assessorNames = assessorNames.stream().filter(assessor -> assessor.equals(capabilityTable.getAssessorFilter())).collect(Collectors.toList());
        } else {
            if (assessorNames.isEmpty()) {
                throw new InvalidDataException("There are no assessors defined in column " + capabilityTable.getAssessorColumn().getColumnName());
            } else {
                assessorNames.subList(1, assessorNames.size()).clear();
            }
        }

        //Sort alphabetically
        Collections.sort(levelNames, new NaturalOrderComparator());
        Collections.sort(processNames, new NaturalOrderComparator());

        //Get only specific level - chosen by parameter
        if (capabilityTable.getSpecificLevel() != null) {
            if (levelNames.size() >= capabilityTable.getSpecificLevel()) {
                levelNames = Arrays.asList(levelNames.get(capabilityTable.getSpecificLevel() - 1));
            } else {
                levelNames.clear();
            }
        } else {
            //Get only first N levels - limited by parameter
            levelNames = levelNames.stream().limit(capabilityTable.getLevelLimit()).collect(Collectors.toList());
        }

        //MultiKey map to store value for each process, level and criterion combination - {(process, level, criterion) : value}
        MultiKeyMap valuesMap = new MultiKeyMap();
        for (int i = 0; i < capabilityTable.getScoreColumn().getSourceData().size(); i++) {
            String processValue = capabilityTable.getProcessColumn().getSourceData().get(i).getValue();
            String assessorValue = capabilityTable.getAssessorColumn().getSourceData().get(i).getValue();
            String levelValue = capabilityTable.getLevelColumn().getSourceData().get(i).getValue();
            String criterionValue = capabilityTable.getCriterionColumn().getSourceData().get(i).getValue();
            String scoreValue = capabilityTable.getScoreColumn().getSourceData().get(i).getValue();

            //Filter by assessor
            if (!assessorNames.contains(assessorValue)) {
                continue;
            }
            //Filter by level
            if (!levelNames.contains(levelValue)) {
                continue;
            }

            //Update map of criterions for each level - map keeps track of all criterions defined in each level
            if (levelAttributesMap.containsKey(levelValue)) {
                levelAttributesMap.get(levelValue).add(criterionValue);
            } else {
                levelAttributesMap.put(levelValue, new LinkedHashSet<>());
                levelAttributesMap.get(levelValue).add(criterionValue);
            }

            //Store value in map
            MultiKey key = new MultiKey(processValue, levelValue, criterionValue);
            if (valuesMap.containsKey(key)) {
                ((ArrayList<String>) valuesMap.get(key)).add(scoreValue);
            } else {
                valuesMap.put(key, new ArrayList(Arrays.asList(scoreValue)));
            }
        }

        //Sort performance criterions for each level - criterions need to be sorted in result table
        for (var levelAttributesKey : levelAttributesMap.keySet()) {
            ArrayList<String> array = new ArrayList<>(levelAttributesMap.get(levelAttributesKey));
            Collections.sort(array, new NaturalOrderComparator());
            levelAttributesMap.put(levelAttributesKey, new LinkedHashSet<>(array));
        }

        //Get all column names (Process Name, criterion1, ...)
        columnArray.add("Process Name");
        for (String key : levelNames) {
            LinkedHashSet<String> criterionsList = levelAttributesMap.get(key);
            for (var criterion : criterionsList) {
                columnArray.add(criterion);
            }
        }

        //Create object with data for jasper table element
        int rows = processNames.size();
        int columns = columnArray.size();
        Object[][] test = new Object[rows][columns];
        int rowIndex = 0;
        for (String processName : processNames) {
            int columnIndex = 0;
            test[rowIndex][columnIndex] = processName;
            columnIndex++;

            for (var levelKey : levelAttributesMap.keySet()) {
                for (var criterion : levelAttributesMap.get(levelKey)) {
                    List<Double> scoresListDouble = new ArrayList<>();
                    if (valuesMap.containsKey(new MultiKey(processName, levelKey, criterion))) {
                        ArrayList<String> scoresList = (ArrayList<String>) valuesMap.get(new MultiKey(processName, levelKey, criterion));
                        for (String score : scoresList) {
                            if (score != null) {
                                try {
                                    Double doubleScore = getValueForScore(score);
                                    scoresListDouble.add(doubleScore);
                                } catch (Exception e) {
                                    throw new JasperReportException("Capability table score column contains unknown value: " + score, e);
                                }
                            }
                        }
                    }

                    Double finalScore = applyScoreFunction(scoresListDouble, capabilityTable.getAggregateScoresFunction());
                    //If process does not have this criterion measured
                    if (finalScore == null) {
                        test[rowIndex][columnIndex] = "";
                    } else {
                        test[rowIndex][columnIndex] = getScoreForValue(finalScore);
                    }

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
