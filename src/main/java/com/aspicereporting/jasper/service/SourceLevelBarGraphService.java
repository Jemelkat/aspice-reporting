package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.enums.Orientation;
import com.aspicereporting.entity.items.SourceLevelBarGraph;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.exception.JasperReportException;
import com.aspicereporting.repository.SourceRepository;
import com.aspicereporting.utils.NaturalOrderComparator;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRenderable;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.renderers.JCommonDrawableRendererImpl;
import net.sf.jasperreports.renderers.Renderable;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SourceLevelBarGraphService extends BaseChartService {
    @Autowired
    SourceRepository sourceRepository;

    public JRDesignImage createElement(JasperDesign jasperDesign, SourceLevelBarGraph sourceLevelBarGraph, Integer counter, Map<String, Object> parameters) throws JRException {
        LinkedHashMap<String, Map<String, Integer>> graphData = getData(sourceLevelBarGraph);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (var process : graphData.keySet()) {
            for (var assessor : graphData.get(process).keySet()) {
                Integer level = graphData.get(process).get(assessor);
                dataset.addValue(level, process, assessor);
            }
        }


        final JFreeChart chart = ChartFactory.createBarChart("",                                   // chart title
                "Process",                  // domain axis label
                "Level",                     // range axis label
                dataset,                            // data
                sourceLevelBarGraph.getOrientation().equals(Orientation.VERTICAL) ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL,           // the plot orientation
                true,                        // legend
                false,                        // tooltips
                false                        // urls
        );
        this.applyBarGraphTheme(chart);

        //Rotate x axis names to save space
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setUpperBound(5);
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        if (sourceLevelBarGraph.getOrientation().equals(Orientation.HORIZONTAL)) {
            CategoryAxis categoryAxis = plot.getDomainAxis();
            categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        }

        //Add data to parameter and add parameter to design
        parameters.put(CHART + counter, new JCommonDrawableRendererImpl(chart));
        JRDesignParameter parameter = new JRDesignParameter();
        parameter.setValueClass(Renderable.class);
        parameter.setName(CHART + counter);
        jasperDesign.addParameter(parameter);

        //Add image - chart will be displayed in image tag
        JRDesignImage imageElement = new JRDesignImage(jasperDesign);
        imageElement.setX(sourceLevelBarGraph.getX());
        imageElement.setY(sourceLevelBarGraph.getY());
        imageElement.setWidth(sourceLevelBarGraph.getWidth());
        imageElement.setHeight(sourceLevelBarGraph.getHeight());
        imageElement.setPositionType(PositionTypeEnum.FLOAT);
        imageElement.setScaleImage(ScaleImageEnum.FILL_FRAME);
        imageElement.setLazy(true);
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("$P{" + CHART + counter + "}");
        expression.setValueClass(JRRenderable.class);
        imageElement.setExpression(expression);

        return imageElement;
    }

    public LinkedHashMap<String, Map<String, Integer>> getData(SourceLevelBarGraph sourceLevelBarGraph) {
        //Stores all existing processes across sources
        Set<String> allProcessSet = new HashSet<>();

        //Use process filters as process names - if they are not defined array will stay empty
        List<String> processFilter = new ArrayList<>(sourceLevelBarGraph.getProcessFilter());
        processFilter = processFilter.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        Collections.sort(processFilter, new NaturalOrderComparator());
        allProcessSet.addAll(processFilter);

        //Contains process: level for each process in source
        LinkedHashMap<String, Map<String, Map<String, Integer>>> levelsAchievedMap = new LinkedHashMap<>();
        for (Source source : sourceLevelBarGraph.getSources()) {
            SourceColumn assessorColumn = getSourceColumnByName(source, sourceLevelBarGraph.getAssessorColumn());
            SourceColumn processColumn = getSourceColumnByName(source, sourceLevelBarGraph.getProcessColumn());
            SourceColumn criterionColumn = getSourceColumnByName(source, sourceLevelBarGraph.getCriterionColumn());
            SourceColumn attributeColumn = getSourceColumnByName(source, sourceLevelBarGraph.getAttributeColumn());
            SourceColumn scoreColumn = getSourceColumnByName(source, sourceLevelBarGraph.getScoreColumn());

            //Get all process names if process filter is not defined
            if (sourceLevelBarGraph.getProcessFilter().isEmpty()) {
                List<String> currentProcessNames = sourceRepository.findDistinctColumnValuesForColumn(processColumn.getId());
                currentProcessNames = currentProcessNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
                allProcessSet.addAll(currentProcessNames);
                processFilter.addAll(currentProcessNames);
                Collections.sort(processFilter, new NaturalOrderComparator());
            }

            //Stores all assessors in this source file
            Set<String> assessorsSet = new HashSet<>();

            // Get all data to MAP for faster lookup*
            MultiKeyMap valuesMap = prepareDataMap(sourceLevelBarGraph, scoreColumn, processColumn, attributeColumn, criterionColumn, assessorColumn, processFilter, assessorsSet);

            if (sourceLevelBarGraph.isMergeLevels()) {
                LinkedHashMap<String, HashMap<String, Integer>> assesorLevelMap = new LinkedHashMap<>();
                for (String process : processFilter) {
                    for (String assessor : assessorsSet) {
                        resetVariables();
                        for (int i = 1; i <= 5; i++) {
                            if (!previousLevelAchieved) {
                                break;
                            }
                            resetCheckVariable();
                            for (String attribute : processAttributesMap.get(i)) {
                                MultiKey multikey = new MultiKey(process, attribute);
                                if (!valuesMap.containsKey(multikey)) {
                                    break;
                                }

                                Map<String, Map<String, String>> criterionAssessorMap = (Map<String, Map<String, String>>) valuesMap.get(multikey);
                                //Get all criterion scores for (process, attribute) key and apply score function on them
                                List<String> stringScoresList = new ArrayList<>();
                                for (String criterion : criterionAssessorMap.keySet()) {
                                    if (criterionAssessorMap.get(criterion).containsKey(assessor)) {
                                        stringScoresList.add(criterionAssessorMap.get(criterion).get(assessor));
                                    } else {
                                        stringScoresList.add("0");
                                    }
                                }
                                List<Double> scoresList;
                                try {
                                    scoresList = convertScoresToDoubles(stringScoresList);
                                } catch (JasperReportException e) {
                                    throw new JasperReportException("Sources level bar graph score column contains unknown value: ", e);
                                }
                                calculateLevelCheckValue(scoresList.stream().mapToDouble(a -> a).sum() / scoresList.size(), i);
                            }
                            evaulateLevelCheckToLevel();
                        }
                        //Add all this process level achieved to map
                        if (assesorLevelMap.containsKey(process)) {
                            assesorLevelMap.get(process).put(assessor, levelAchieved);
                        } else {
                            HashMap<String, Integer> levelMap = new HashMap<>(Map.of(assessor, levelAchieved));
                            assesorLevelMap.put(process, levelMap);
                        }
                    }
                }

                //TODO add merge function and add to levelsAchievedMap
                for (String process : assesorLevelMap.keySet()) {
                    List<Integer> processLevels = List.copyOf(assesorLevelMap.get(process).values());
                    Integer level;
                    switch (sourceLevelBarGraph.getScoreFunction()) {
                        case MIN:
                            level = applyMinMaxFunction(processLevels, sourceLevelBarGraph.getScoreFunction());
                            if (levelsAchievedMap.containsKey(source.getSourceName())) {
                                HashMap<String, Map<String, Integer>> assessorMap = (HashMap<String, Map<String, Integer>>) levelsAchievedMap.get(source.getSourceName());
                                assessorMap.get("test").put(process, level);
                            } else {
                                HashMap<String, Integer> processMap = new HashMap<>(Map.of(process, level));
                                HashMap<String, Map<String, Integer>> assessorMap = new HashMap<>(Map.of("test", processMap));
                                levelsAchievedMap.put(source.getSourceName(), assessorMap);
                            }
                            break;
                        case MAX:
                            level = applyMinMaxFunction(processLevels, sourceLevelBarGraph.getScoreFunction());
                            if (levelsAchievedMap.containsKey(source.getSourceName())) {
                                HashMap<String, Map<String, Integer>> assessorMap = (HashMap<String, Map<String, Integer>>) levelsAchievedMap.get(source.getSourceName());
                                assessorMap.get("test").put(process, level);
                            } else {
                                HashMap<String, Integer> processMap = new HashMap<>(Map.of(process, level));
                                HashMap<String, Map<String, Integer>> assessorMap = new HashMap<>(Map.of("test", processMap));
                                levelsAchievedMap.put(source.getSourceName(), assessorMap);
                            }
                            break;
                        default:
                            break;
                    }
                }


            } else {
                for (String process : processFilter) {
                    resetVariables();
                    for (int i = 1; i <= 5; i++) {
                        if (!previousLevelAchieved) {
                            break;
                        }
                        resetCheckVariable();
                        for (String attribute : processAttributesMap.get(i)) {
                            MultiKey multikey = new MultiKey(process, attribute);
                            //Process does not have this attribute defined we don't have to increase level
                            if (!valuesMap.containsKey(multikey)) {
                                break;
                            }

                            //Get all evaluated criterions for this process attribute
                            Map<String, Map<String, String>> criterionAssessorMap = (Map<String, Map<String, String>>) valuesMap.get(multikey);
                            //Get all criterion scores for (process, attribute) key and apply score function on them
                            List<Double> scoresList = new ArrayList<>();
                            for (String criterion : criterionAssessorMap.keySet()) {
                                List<String> assessorScoreList = new ArrayList<>();
                                for (String assessor : criterionAssessorMap.get(criterion).keySet()) {
                                    assessorScoreList.add(criterionAssessorMap.get(criterion).get(assessor));
                                }
                                try {
                                    scoresList.add(applyScoreFunction(convertScoresToDoubles(assessorScoreList), sourceLevelBarGraph.getScoreFunction()));
                                } catch (JasperReportException e) {
                                    throw new JasperReportException("Sources level bar graph score column contains unknown value: ", e);
                                }
                            }
                            //Get attribute score achieved as (sum of scores/count of scores)
                            calculateLevelCheckValue(scoresList.stream().mapToDouble(a -> a).sum() / scoresList.size(), i);
                        }
                        evaulateLevelCheckToLevel();
                    }
                    //Add all this process level achieved to map
                    if (levelsAchievedMap.containsKey(source.getSourceName())) {
                        HashMap<String, Map<String, Integer>> assessorMap = (HashMap<String, Map<String, Integer>>) levelsAchievedMap.get(source.getSourceName());
                        assessorMap.get("test").put(process, levelAchieved);
                    } else {
                        HashMap<String, Integer> processMap = new HashMap<>(Map.of(process, levelAchieved));
                        HashMap<String, Map<String, Integer>> assessorMap = new HashMap<>(Map.of("test", processMap));
                        levelsAchievedMap.put(source.getSourceName(), assessorMap);
                    }
                }
            }
        }
        //Result data in format {sourcename1: {process1: level, process2: level}, sourceName2: ...}
        LinkedHashMap<String, Map<String, Integer>> dataMap = new LinkedHashMap<>();

        ArrayList<String> allProcessList = new ArrayList<>(allProcessSet);
        Collections.sort(allProcessList, new NaturalOrderComparator());
        for (String process : allProcessList) {
            //Get levels for process across all sources
            List<String> sourceNames = new ArrayList<>();
            List<Integer> processLevels = new ArrayList<>();
            for (String source : levelsAchievedMap.keySet()) {
                sourceNames.add(source);
                if (levelsAchievedMap.get(source).get("test").containsKey(process)) {
                    processLevels.add(levelsAchievedMap.get(source).get("test").get(process));
                } else {
                    processLevels.add(0);
                }

            }

            //Merge process levels or create record for each source
            switch (sourceLevelBarGraph.getMergeScores()) {
                case MAX:
                    if (!dataMap.containsKey("MAX levels")) {
                        dataMap.put("MAX levels", new LinkedHashMap<>());
                    }
                    dataMap.get("MAX levels").put(process, applyMinMaxFunction(processLevels, sourceLevelBarGraph.getMergeScores()));
                    break;
                case MIN:
                    if (!dataMap.containsKey("MIN levels")) {
                        dataMap.put("MIN levels", new LinkedHashMap<>());
                    }
                    dataMap.get("MIN levels").put(process, applyMinMaxFunction(processLevels, sourceLevelBarGraph.getMergeScores()));
                    break;
                default:
                    for (int i = 0; i < sourceNames.size(); i++) {
                        String source = sourceNames.get(i);
                        Integer level = processLevels.get(i);
                        if (!dataMap.containsKey(source)) {
                            dataMap.put(source, new LinkedHashMap<>());
                        }
                        dataMap.get(source).put(process, level);
                    }
                    break;
            }
        }

        //Fill all missing processes with level 0
        for (String process : allProcessList) {
            for (var dataKey : dataMap.keySet()) {
                if (!dataMap.get(dataKey).containsKey(process)) {
                    dataMap.get(dataKey).put(process, 0);
                }
            }
        }

        return dataMap;
    }

    /**
     * Returns data in map format for easier lookup
     * {(process,attribute): [{criterion1:[{assessor1: score,assessor2: score}]},{criterion2: [{assessor1: score,assessor2: score}]},...}
     */
    private MultiKeyMap prepareDataMap(SourceLevelBarGraph sourceLevelBarGraph, SourceColumn scoreColumn, SourceColumn processColumn, SourceColumn attributeColumn, SourceColumn criterionColumn, SourceColumn assessorColumn, List<String> processFilter, Set<String> assessorsSet) {
        //Precompile regex pattern for assessor regex filter
        Pattern assessorPattern = Pattern.compile(StringUtils.isEmpty(sourceLevelBarGraph.getAssessorFilter()) ? "" : sourceLevelBarGraph.getAssessorFilter());
        Matcher assessorMatcher = assessorPattern.matcher("");

        MultiKeyMap valuesMap = new MultiKeyMap();
        for (int i = 0; i < scoreColumn.getSourceData().size(); i++) {
            String process = processColumn.getSourceData().get(i).getValue();
            String attribute = attributeColumn.getSourceData().get(i).getValue();
            String criterion = criterionColumn.getSourceData().get(i).getValue();
            String score = scoreColumn.getSourceData().get(i).getValue();
            String assessor = assessorColumn.getSourceData().get(i).getValue();

            //Filter by process
            if (!sourceLevelBarGraph.getProcessFilter().isEmpty()) {
                if (!processFilter.contains(process)) {
                    continue;
                }
            }
            //Filter by assessor
            if (!StringUtils.isEmpty(sourceLevelBarGraph.getAssessorFilter())) {
                assessorMatcher.reset(assessor);
                if (!assessorMatcher.matches()) {
                    assessorsSet.add(assessor);
                    continue;
                }
            } else {
                assessorsSet.add(assessor);
            }

            MultiKey key = new MultiKey(process, attribute);
            //If we already have record for this process attribute combination
            if (valuesMap.containsKey(key)) {
                Map<String, Map<String, String>> criterionMap = (Map<String, Map<String, String>>) valuesMap.get(key);
                //If we already have this criterion recorded - add new score for assesor
                if (criterionMap.containsKey(criterion)) {
                    criterionMap.get(criterion).put(assessor, score);
                } else {
                    criterionMap.put(criterion, new HashMap(Map.of(assessor, score)));
                }
            }
            //Create new criterion score record for this assessor
            else {
                valuesMap.put(key, new HashMap<String, HashMap<String, String>>());
                ((Map<String, Map<String, String>>) valuesMap.get(key)).put(criterion, new HashMap(Map.of(assessor, score)));
            }
        }
        return valuesMap;
    }

    private SourceColumn getSourceColumnByName(Source source, String name) {
        for (SourceColumn sourceColumn : source.getSourceColumns()) {
            if (sourceColumn.getColumnName().equals(name)) {
                return sourceColumn;
            }
        }
        throw new InvalidDataException("Source level bar graph source: " + source.getSourceName() + " has no column named: " + name);
    }
}
