package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.enums.Orientation;
import com.aspicereporting.entity.enums.ScoreFunction;
import com.aspicereporting.entity.items.LevelBarGraph;
import com.aspicereporting.exception.JasperReportException;
import com.aspicereporting.repository.SourceColumnRepository;
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
import java.util.stream.Collectors;

@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class LevelBarGraphService extends BaseChartService {
    @Autowired
    SourceRepository sourceRepository;
    @Autowired
    SourceColumnRepository sourceColumnRepository;

    public JRDesignImage createElement(JasperDesign jasperDesign, LevelBarGraph levelBarGraph, Integer counter, Map<String, Object> parameters) throws JRException {
        //Get data
        LinkedHashMap<String, Map<String, Integer>> graphData = getData(levelBarGraph);

        //Create dataset for chart
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (var process : graphData.keySet()) {
            for (var assessor : graphData.get(process).keySet()) {
                Integer level = graphData.get(process).get(assessor);
                dataset.addValue(level, assessor, process);
            }
        }

        final JFreeChart chart = ChartFactory.createBarChart(
                "",                                   // chart title
                "Process",                  // domain axis label
                "Level",                     // range axis label
                dataset,                            // data
                levelBarGraph.getOrientation().equals(Orientation.VERTICAL) ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL,           // the plot orientation
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
        if (levelBarGraph.getOrientation().equals(Orientation.HORIZONTAL)) {
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
        imageElement.setX(levelBarGraph.getX());
        imageElement.setY(levelBarGraph.getY());
        imageElement.setWidth(levelBarGraph.getWidth());
        imageElement.setHeight(levelBarGraph.getHeight());
        imageElement.setPositionType(PositionTypeEnum.FLOAT);
        imageElement.setScaleImage(ScaleImageEnum.FILL_FRAME);
        imageElement.setLazy(true);
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("$P{" + CHART + counter + "}");
        expression.setValueClass(JRRenderable.class);
        imageElement.setExpression(expression);

        return imageElement;
    }

    public LinkedHashMap<String, Map<String, Integer>> getData(LevelBarGraph levelBarGraph) {
        //Get all unique processes and assessors
        List<String> processFilter = sourceRepository.findDistinctColumnValuesForColumn(levelBarGraph.getProcessColumn().getId());
        List<String> assessorFilter = sourceRepository.findDistinctColumnValuesForColumn(levelBarGraph.getAssessorColumn().getId());

        //Remove empty levels "" and processes ""
        assessorFilter = assessorFilter.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        processFilter = processFilter.stream().filter(name -> !name.equals("")).collect(Collectors.toList());

        //Apply process filter to process list
        if (!levelBarGraph.getProcessFilter().isEmpty()) {
            processFilter = processFilter.stream().filter(process -> levelBarGraph.getProcessFilter().contains(process)).collect(Collectors.toList());
        }
        //Apply assessor filter to assessor list
        if (!levelBarGraph.getAssessorFilter().isEmpty()) {
            assessorFilter = assessorFilter.stream().filter(assessor -> levelBarGraph.getAssessorFilter().contains(assessor)).collect(Collectors.toList());
        }
        //Sort alphabetically
        Collections.sort(processFilter, new NaturalOrderComparator());


        //Get values map for easier lookup
        MultiKeyMap valuesMap = prepareDataMap(levelBarGraph, assessorFilter, processFilter);

        //Get map with levels for each process and assessor combination
        LinkedHashMap<String, Map<String, Integer>> processLevelMap = prepareLevelsMap(levelBarGraph, valuesMap, assessorFilter, processFilter);

        return processLevelMap;
    }

    private MultiKeyMap prepareDataMap(LevelBarGraph levelBarGraph, List<String> assessorFilter, List<String> processFilter) {
        //MultiKey map to store value for each process, level combination - {(process, attribute, assessor) : (criterion: [values])}
        MultiKeyMap valuesMap = new MultiKeyMap();
        for (int i = 0; i < levelBarGraph.getScoreColumn().getSourceData().size(); i++) {
            String process = levelBarGraph.getProcessColumn().getSourceData().get(i).getValue();
            String criterion = levelBarGraph.getCriterionColumn().getSourceData().get(i).getValue();
            String attribute = levelBarGraph.getAttributeColumn().getSourceData().get(i).getValue();
            String score = levelBarGraph.getScoreColumn().getSourceData().get(i).getValue();
            String assessor = levelBarGraph.getAssessorColumn().getSourceData().get(i).getValue();

            //Filter by assessor and process
            if (!assessorFilter.contains(assessor) || (!processFilter.contains(process))) {
                continue;
            }

            MultiKey key = new MultiKey(process, attribute);
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

    /**
     * Method returns map with levels achieved for each process
     * This method performs merging of assessors based on defined score function and merge checkbox
     * @param levelBarGraph
     * @param valuesMap
     * @param assessorFilter
     * @param processFilter
     * @return returns map {process: assessor: level}
     */
    private LinkedHashMap<String, Map<String, Integer>> prepareLevelsMap(LevelBarGraph levelBarGraph, MultiKeyMap valuesMap, List<String> assessorFilter, List<String> processFilter) {
        LinkedHashMap<String, Map<String, Integer>> processLevelMap = new LinkedHashMap<>();
        //Get data not merged
        if (levelBarGraph.getScoreFunction().equals(ScoreFunction.NONE) || levelBarGraph.isMergeLevels()) {
            for (var process : processFilter) {
                for (var assessor : assessorFilter) {
                    //Check if this assessor evaulated this process
                    boolean hasRecord = false;
                    resetVariables();
                    for (int i = 1; i <= 5; i++) {
                        //If previous level is not fully achieved move to another process
                        if (!previousLevelAchieved) {
                            break;
                        }
                        //Reset check variable used for determining of level achieved
                        resetCheckVariable();
                        for (String attribute : processAttributesMap.get(i)) {
                            MultiKey multikey = new MultiKey(process, attribute);
                            //Process does not have this attribute defined we dont have to increase level
                            if (!valuesMap.containsKey(multikey)) {
                                break;
                            }

                            //Get all criterion scores for (process, attribute, assessor) key and apply score function on them
                            Map<String, Map<String, String>> criterionAssessorMap = (Map<String, Map<String, String>>) valuesMap.get(multikey);
                            List<String> stringScoreList = new ArrayList<>();
                            for (String criterionKey : criterionAssessorMap.keySet()) {
                                if (criterionAssessorMap.get(criterionKey).containsKey(assessor)) {
                                    hasRecord = true;
                                    stringScoreList.add(criterionAssessorMap.get(criterionKey).get(assessor));
                                }
                            }

                            List<Double> scoresList;
                            try {
                                scoresList = convertScoresToDoubles(stringScoreList);
                            } catch (JasperReportException e) {
                                throw new JasperReportException("Level bar graph score column contains unknown value: ", e);
                            }

                            //Get attribute score achieved as (sum of scores/count of scores)
                            Integer listSize = scoresList.isEmpty() ? 1 : scoresList.size();
                            calculateLevelCheckValue(scoresList.stream().mapToDouble(a -> a).sum() / listSize, i);
                        }
                        evaulateLevelCheckToLevel();
                    }

                    if (hasRecord) {
                        if (processLevelMap.containsKey(process)) {
                            processLevelMap.get(process).put(assessor, levelAchieved);
                        } else {
                            processLevelMap.put(process, new HashMap<>(Map.of(assessor, levelAchieved)));
                        }
                    }
                }
            }

            if (levelBarGraph.isMergeLevels()) {
                processLevelMap = mergeAssessors(processLevelMap, levelBarGraph.getScoreFunction());
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
                                scoresList.add(applyScoreFunction(convertScoresToDoubles(assessorScoreList), levelBarGraph.getScoreFunction()));
                            } catch (JasperReportException e) {
                                throw new JasperReportException("Sources level bar graph score column contains unknown value: ", e);
                            }
                        }
                        //Get attribute score achieved as (sum of scores/count of scores)
                        calculateLevelCheckValue(scoresList.stream().mapToDouble(a -> a).sum() / scoresList.size(), i);
                    }
                    evaulateLevelCheckToLevel();
                }

                if (processLevelMap.containsKey(process)) {
                    processLevelMap.get(process).put(levelBarGraph.getScoreFunction().name() + " scores achieved", levelAchieved);
                } else {
                    processLevelMap.put(process, new HashMap<>(Map.of(levelBarGraph.getScoreFunction().name() + " scores achieved", levelAchieved)));
                }
            }
        }
        return processLevelMap;
    }

    private LinkedHashMap<String, Map<String, Integer>> mergeAssessors(LinkedHashMap<String, Map<String, Integer>> processLevelMap, ScoreFunction scoreFunction) {
        LinkedHashMap<String, Map<String, Integer>> updatedMap = new LinkedHashMap<>();
        for (String process : processLevelMap.keySet()) {
            List<Integer> levels = List.copyOf(processLevelMap.get(process).values());
            updatedMap.put(process, new HashMap<>(Map.of(scoreFunction.name() + " levels achieved", applyMinMaxFunction(levels, scoreFunction))));
        }
        return updatedMap;
    }
}
