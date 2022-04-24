package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.items.CapabilityTable;
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
import java.util.List;
import java.util.*;
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

    /**
     * Creates JRDesign element which can be added to JasperDesign
     */
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
        SimpleTableModel tableModel = getData(capabilityTable, levelAttributesMap);
        parameters.put(TABLE_DATA + tableCount, new JRTableModelDataSource(tableModel));

        //Add fields for columns
        JRDesignField processField = new JRDesignField();
        processField.setValueClass(String.class);
        processField.setName(columnArray.get(0));
        tableSubdataset.addField(processField);
        if (levelAttributesMap.isEmpty()) {
            JRDesignField field = new JRDesignField();
            field.setValueClass(String.class);
            field.setName(columnArray.get(1));
            tableSubdataset.addField(field);
        } else {
            for (var key : levelAttributesMap.keySet()) {
                for (var column : levelAttributesMap.get(key)) {
                    JRDesignField field = new JRDesignField();
                    field.setValueClass(String.class);
                    field.setName(column);
                    tableSubdataset.addField(field);
                }
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
        if (levelAttributesMap.isEmpty()) {
            StandardColumn sc = createCapabilityColumns(jasperDesign, 5 * capabilityTable.getCriterionWidth(), rowHeight, fontSize, "No criterions found", "No criterions found", true);
            StandardColumnGroup columnGroup = new StandardColumnGroup();
            columnGroup.setWidth(5 * capabilityTable.getCriterionWidth());
            DesignCell header = new DesignCell();
            header.setDefaultStyleProvider(jasperDesign);
            header.getLineBox().getPen().setLineWidth(1f);
            header.setHeight(rowHeight);
            JRDesignStaticText headerElement = new JRDesignStaticText(jasperDesign);
            headerElement.setX(0);
            headerElement.setY(0);
            headerElement.setWidth(5 * capabilityTable.getCriterionWidth());
            headerElement.setHeight(rowHeight);
            headerElement.setText("Levels");
            headerElement.setFontSize(fontSize);
            headerElement.setFontName("DejaVu Serif");
            headerElement.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
            headerElement.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            header.addElement(headerElement);
            columnGroup.setColumnHeader(header);
            columnGroup.addColumn(sc);
            table.addColumn(columnGroup);
        } else {
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
        }

        tableElement.setComponent(table);
        return tableElement;
    }
    /**
     * Gets all result data based on item settings
     */
    public SimpleTableModel getData(CapabilityTable capabilityTable, Map<String, LinkedHashSet<String>> levelAttributesMap) {
        //Initialize score ranges based on source definitions
        initializeScoreRanges(capabilityTable.getSource().getScoreRange());

        //Get all unique processes, levels and assessors
        List<String> assessorFilter = sourceRepository.findDistinctColumnValuesForColumn(capabilityTable.getAssessorColumn().getId());
        List<String> processFilter = new ArrayList<>(capabilityTable.getProcessFilter());

        //Remove empty ""
        processFilter = processFilter.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        assessorFilter = assessorFilter.stream().filter(name -> !name.equals("")).collect(Collectors.toList());

        //Apply assessor filter
        if (!capabilityTable.getAssessorFilter().isEmpty()) {
            assessorFilter = assessorFilter.stream().filter(assessor -> capabilityTable.getAssessorFilter().contains(assessor)).collect(Collectors.toList());
        }

        Set<String> allProcessSet = new HashSet<>();
        allProcessSet.addAll(processFilter);
        //Sort alphabetically
        Collections.sort(processFilter, new NaturalOrderComparator());

        //Get all relevant data
        MultiKeyMap valuesMap = prepareDataMap(capabilityTable, processFilter, assessorFilter, levelAttributesMap, allProcessSet);

        //Apply score aggregation function to data
        valuesMap = aggregateScores(capabilityTable, valuesMap);

        //Create table model for jasper from data
        return prepareDataModel(valuesMap, levelAttributesMap, allProcessSet);
    }

    /**
     * Returns data in map format for easier lookup
     * This method also updates criterions in levelCriterionsMap - this map keeps track of all found criterions for each level
     * {(process,attribute, criterion): [scoreAssesor1, scoreAssesor2, scoreAssesor3],...}
     */
    private MultiKeyMap prepareDataMap(CapabilityTable capabilityTable, List<String> processFilter, List<String> assessorFilter, Map<String, LinkedHashSet<String>> levelCriterionsMap, Set<String> allProcessSet) {
        //MultiKey map to store value for each process, level and criterion combination - {(process, level, criterion) : value}
        Set<String> allLevels = new HashSet<>();
        MultiKeyMap valuesMap = new MultiKeyMap();
        for (int i = 0; i < capabilityTable.getScoreColumn().getSourceData().size(); i++) {
            String process = capabilityTable.getProcessColumn().getSourceData().get(i).getValue();
            String assessor = capabilityTable.getAssessorColumn().getSourceData().get(i).getValue();
            String level = capabilityTable.getLevelColumn().getSourceData().get(i).getValue();
            String criterion = capabilityTable.getCriterionColumn().getSourceData().get(i).getValue();
            String score = capabilityTable.getScoreColumn().getSourceData().get(i).getValue();

            //Filter by process
            if (!capabilityTable.getProcessFilter().isEmpty()) {
                if (!processFilter.contains(process)) {
                    continue;
                }
            }
            //Filter by assessor
            if (!capabilityTable.getAssessorFilter().isEmpty()) {
                if (!assessorFilter.contains(assessor)) {
                    continue;
                }
            }
            if(process.equals("")) {
                continue;
            }
            allProcessSet.add(process);
            allLevels.add(level);

            //Store scores in map
            MultiKey key = new MultiKey(process, level, criterion);
            if (valuesMap.containsKey(key)) {
                ((List<String>) valuesMap.get(key)).add(score);
            } else {
                valuesMap.put(key, new ArrayList<>(List.of(score)));
            }
        }

        List<String> allLevelsList = allLevels.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        Collections.sort(allLevelsList, new NaturalOrderComparator());

        //Return only specific or max N levels
        if (capabilityTable.getSpecificLevel() != null) {
            if (capabilityTable.getSpecificLevel() > allLevelsList.size()) {
                valuesMap.clear();
            } else {
                String specificLevel = allLevelsList.get(capabilityTable.getSpecificLevel() - 1);
                for (var key : new HashSet<>(valuesMap.keySet())) {
                    MultiKey multiKey = (MultiKey) key;
                    if (!specificLevel.equals(multiKey.getKey(1))) {
                        valuesMap.remove(multiKey);
                    }
                }
            }
        } else {
            //Get only first N levels - limited by parameter
            List<String> firstNLevels = allLevelsList.stream().limit(capabilityTable.getLevelLimit()).collect(Collectors.toList());
            for (var key : new HashSet<>(valuesMap.keySet())) {
                MultiKey multiKey = (MultiKey) key;
                if (!firstNLevels.contains(multiKey.getKey(1))) {
                    valuesMap.remove(multiKey);
                }
            }
        }

        //Add criterions to coresponding levels in levelCriterionsMap
        for (var key : new HashSet<>(valuesMap.keySet())) {
            MultiKey multiKey = (MultiKey) key;
            //Update map of criterions for each level - map keeps track of all criterions defined in each level
            String level = (String) multiKey.getKey(1);
            if (levelCriterionsMap.containsKey(level)) {
                levelCriterionsMap.get(level).add((String) multiKey.getKey(2));
            } else {
                levelCriterionsMap.put(level, new LinkedHashSet<>());
                levelCriterionsMap.get(level).add((String) multiKey.getKey(2));
            }
        }

        return valuesMap;
    }

    /**
     * Method creates SimpleTableModel with scores for each process and performance criterion based on data in valuesMap
     */
    private SimpleTableModel prepareDataModel(MultiKeyMap valuesMap, Map<String, LinkedHashSet<String>> levelCriterionsMap, Set<String> allProcessSet) {
        List<String> sortedCriterions = new ArrayList(levelCriterionsMap.keySet());
        Collections.sort(sortedCriterions, new NaturalOrderComparator());
        //Sort criterion scores for each level - criterions need to be sorted in result table
        for (var levelAttributesKey : sortedCriterions) {
            ArrayList<String> array = new ArrayList<>(levelCriterionsMap.get(levelAttributesKey));
            Collections.sort(array, new NaturalOrderComparator());
            levelCriterionsMap.remove(levelAttributesKey);
            levelCriterionsMap.put(levelAttributesKey, new LinkedHashSet<>(array));
        }

        //Get all column names (Process Name, criterion1, ...)
        columnArray.add("Process Name");
        for (String key : levelCriterionsMap.keySet()) {
            LinkedHashSet<String> criterionsList = levelCriterionsMap.get(key);
            for (var criterion : criterionsList) {
                columnArray.add(criterion);
            }
        }

        //No criterions found - will add no measurements found message later
        if (columnArray.size() == 1) {
            columnArray.add("No criterions found");
        }

        List<String> allProcessList = allProcessSet.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        Collections.sort(allProcessList, new NaturalOrderComparator());

        //Create object with data for jasper table element
        int rows = allProcessList.size();
        int columns = columnArray.size();
        Object[][] dataObject = new Object[rows][columns];
        int rowIndex = 0;
        for (String process : allProcessList) {
            int columnIndex = 0;
            dataObject[rowIndex][columnIndex] = process;
            columnIndex++;

            if (levelCriterionsMap.isEmpty()) {
                dataObject[rowIndex][columnIndex] = "No measurements found";
            } else {
                for (var level : levelCriterionsMap.keySet()) {
                    for (var criterion : levelCriterionsMap.get(level)) {
                        if (valuesMap.containsKey(new MultiKey(process, level, criterion))) {
                            dataObject[rowIndex][columnIndex] = ((List<String>) valuesMap.get(new MultiKey(process, level, criterion))).get(0);
                        } else {
                            dataObject[rowIndex][columnIndex] = "";
                        }
                        columnIndex++;
                    }
                }
            }


            rowIndex++;
        }

        //Creates data bean
        SimpleTableModel tableModel = new SimpleTableModel(rows, columns);
        tableModel.setColumnNames(columnArray.toArray(new String[0]));
        tableModel.setData(dataObject);
        tablesCounter++;
        return tableModel;
    }


    /***
     *  Aggregates all criterion score for each level by defined function
     */
    private MultiKeyMap aggregateScores(CapabilityTable capabilityTable, MultiKeyMap valuesMap) {
        for (var key : new HashSet<>(valuesMap.keySet())) {
            List<String> scores = (List<String>) valuesMap.get(key);
            Double newValue;
            try {
                newValue = applyScoreFunction(convertScoresToDoubles(scores), capabilityTable.getAggregateScoresFunction());
            } catch (JasperReportException e) {
                throw new JasperReportException("Capability table id = " + capabilityTable.getId() + " score column contains unknown value in: " + scores.toString(), e);
            }
            valuesMap.put((MultiKey) key, new ArrayList<>(List.of(getScoreForValue(newValue))));
        }
        return valuesMap;
    }

    private StandardColumn createCapabilityColumns(JasperDesign jasperDesign, int width, int height, float fontSize, String headerText, String detailExpression, boolean isStyled) throws JRException {
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
        JRDesignExpression Lexpression2 = new JRDesignExpression();
        Lexpression2.setText("$F{" + expression + "}.equals(\"L-\")");
        JRDesignConditionalStyle LCondStyle2 = new JRDesignConditionalStyle();
        LCondStyle2.setConditionExpression(Lexpression2);
        LCondStyle2.setBackcolor(new Color(255, 192, 0));
        jrDesignStyle.addConditionalStyle(LCondStyle2);
        JRDesignExpression Lexpression3 = new JRDesignExpression();
        Lexpression3.setText("$F{" + expression + "}.equals(\"L+\")");
        JRDesignConditionalStyle LCondStyle3 = new JRDesignConditionalStyle();
        LCondStyle3.setConditionExpression(Lexpression3);
        LCondStyle3.setBackcolor(new Color(255, 192, 0));
        jrDesignStyle.addConditionalStyle(LCondStyle3);
        //P- orange
        JRDesignExpression Pexpression = new JRDesignExpression();
        Pexpression.setText("$F{" + expression + "}.equals(\"P\")");
        JRDesignConditionalStyle PCondStyle = new JRDesignConditionalStyle();
        PCondStyle.setConditionExpression(Pexpression);
        PCondStyle.setBackcolor(new Color(146, 208, 80));
        jrDesignStyle.addConditionalStyle(PCondStyle);
        JRDesignExpression Pexpression2 = new JRDesignExpression();
        Pexpression2.setText("$F{" + expression + "}.equals(\"P-\")");
        JRDesignConditionalStyle PCondStyle2 = new JRDesignConditionalStyle();
        PCondStyle2.setConditionExpression(Pexpression2);
        PCondStyle2.setBackcolor(new Color(146, 208, 80));
        jrDesignStyle.addConditionalStyle(PCondStyle2);
        JRDesignExpression Pexpression3 = new JRDesignExpression();
        Pexpression3.setText("$F{" + expression + "}.equals(\"P+\")");
        JRDesignConditionalStyle PCondStyle3 = new JRDesignConditionalStyle();
        PCondStyle3.setConditionExpression(Pexpression3);
        PCondStyle3.setBackcolor(new Color(146, 208, 80));
        jrDesignStyle.addConditionalStyle(PCondStyle3);
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
