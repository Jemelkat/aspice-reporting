package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.enums.ScoreFunction;
import com.aspicereporting.entity.items.LevelPieGraph;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class LevelPieGraphService extends BaseChartService {
    @Autowired
    SourceRepository sourceRepository;

    private static Paint[] pieColors = new Paint[]{
            Color.decode("#4572a7"),
            Color.decode("#008fbe"),
            Color.decode("#00aab7"),
            Color.decode("#00c092"),
            Color.decode("#70cf5c"),
            Color.decode("#d6d327")
    };

    /**
     * Creates JRDesignImage (level pie graph) which can be used in JasperDesign
     */
    public JRDesignImage createElement(JasperDesign jasperDesign, LevelPieGraph levelPieGraph, Integer counter, Map<String, Object> parameters) throws JRException {
        //Get data
        LinkedHashMap<String, Integer> graphData = getData(levelPieGraph);

        //Create dataset for chart
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (var level : graphData.keySet()) {
            Integer count = graphData.get(level);
            dataset.setValue(level, count);
        }

        final JFreeChart chart = ChartFactory.createPieChart(
                levelPieGraph.getTitle() != null ? levelPieGraph.getTitle() : "",
                dataset,
                true,
                true,
                false
        );

        applyPieGraphTheme(chart);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setOutlinePaint(null);
        plot.setShadowPaint(null);

        //Create lables on pie sections
        PieSectionLabelGenerator gen = new StandardPieSectionLabelGenerator(
                "{1} ({2})", NumberFormat.getInstance(), NumberFormat.getPercentInstance());
        plot.setSimpleLabels(true);
        plot.setLabelGenerator(gen);
        plot.setLabelBackgroundPaint(Color.white);
        plot.setLabelShadowPaint(null);
        plot.setLabelOutlinePaint(null);

        //Create Legend
        plot.setLegendItemShape(new Rectangle(0, 0, 10, 10));
        chart.getLegend().setFrame(BlockBorder.NONE);

        for (int i = 0; i < dataset.getItemCount(); i++) {
            String key = (String) dataset.getKey(i);
            plot.setSectionPaint(key, pieColors[i]);
            plot.setSectionOutlinePaint(key, Color.white);
        }

        //Add data to parameter and add parameter to design
        parameters.put("chart" + counter, new JCommonDrawableRendererImpl(chart));
        JRDesignParameter parameter = new JRDesignParameter();
        parameter.setValueClass(Renderable.class);
        parameter.setName("chart" + counter);
        jasperDesign.addParameter(parameter);

        //Add image - chart will be displayed in image tag
        JRDesignImage imageElement = new JRDesignImage(jasperDesign);
        imageElement.setX(levelPieGraph.getX());
        imageElement.setY(levelPieGraph.getY());
        imageElement.setWidth(levelPieGraph.getWidth());
        imageElement.setHeight(levelPieGraph.getHeight());
        imageElement.setPositionType(PositionTypeEnum.FLOAT);
        imageElement.setScaleImage(ScaleImageEnum.FILL_FRAME);
        imageElement.setLazy(true);
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("$P{chart" + counter + "}");
        expression.setValueClass(JRRenderable.class);
        imageElement.setExpression(expression);

        return imageElement;
    }

    /**
     * Gets all result data based on item settings
     */
    public LinkedHashMap<String, Integer> getData(LevelPieGraph levelPieGraph) {
        //Initialize score ranges based on source definitions
        initializeScoreRanges(levelPieGraph.getSource().getScoreRange());
        //Get all unique processes and levels
        List<String> assessorFilter = sourceRepository.findDistinctColumnValuesForColumn(levelPieGraph.getAssessorColumn().getId());
        //Remove empty assessors "" and processes ""
        assessorFilter = assessorFilter.stream().filter(name -> !name.equals("")).collect(Collectors.toList());

        //Apply assessor filter
        if (!levelPieGraph.getAssessorFilter().isEmpty()) {
            assessorFilter = assessorFilter.stream().filter(assessor -> levelPieGraph.getAssessorFilter().contains(assessor)).collect(Collectors.toList());
        }


        Set<String> processSet = new HashSet<>();

        MultiKeyMap valuesMap = prepareDataMap(levelPieGraph, processSet, assessorFilter);
        List<String> processList = new ArrayList<>(processSet);
        //Sort alphabetically
        Collections.sort(processList, new NaturalOrderComparator());

        LinkedHashMap<String, Integer> processLevelMap;
        if (levelPieGraph.isAggregateLevels()) {
            processLevelMap = getLevelsByLevel(levelPieGraph, valuesMap, processList, assessorFilter);
        } else {
            processLevelMap = getLevelsByScore(levelPieGraph, valuesMap, processList);
        }

        //Initialize counts for each level
        LinkedHashMap<String, Integer> graphData = new LinkedHashMap<>(Map.of("0", 0, "1", 0, "2", 0, "3", 0, "4", 0, "5", 0));
        for (String process : processLevelMap.keySet()) {
            Integer level = processLevelMap.get(process);
            graphData.put(level.toString(), graphData.get(level.toString()) + 1);
        }

        //Reword keys from numbers to "Level 0" and remove levels with count 0
        for (var key : new LinkedHashMap<>(graphData).keySet()) {
            Integer count = graphData.get(key);
            if (count == 0) {
                graphData.remove(key);
            } else {
                graphData.remove(key);
                graphData.put("Level " + key, count);
            }
        }

        if (!graphData.isEmpty()) {
            //Sort entries - entry with the highest count will be first
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(graphData.entrySet());
            Collections.sort(entries, Map.Entry.comparingByValue(Comparator.reverseOrder()));

            //Update graphData map with sorted entries
            graphData.clear();
            for (Map.Entry<String, Integer> e : entries) {
                graphData.put(e.getKey(), e.getValue());
            }
        } else {
            graphData.put("No measurements found", 1);
        }

        return graphData;
    }

    /**
     * Returns data in map format for easier lookup
     * {(process,attribute): [{criterion1:[{assessor1: score,assessor2: score}]},{criterion2: [{assessor1: score,assessor2: score}]},...}
     */
    private MultiKeyMap prepareDataMap(LevelPieGraph levelPieGraph, Set<String> processSet, List<String> assessorFilter) {
        //MultiKey map to store value for each process, level combination - {(process, attribute) : (criterion: [value])}
        MultiKeyMap valuesMap = new MultiKeyMap();
        for (int i = 0; i < levelPieGraph.getScoreColumn().getSourceData().size(); i++) {
            String process = levelPieGraph.getProcessColumn().getSourceData().get(i).getValue();
            String criterion = levelPieGraph.getCriterionColumn().getSourceData().get(i).getValue();
            String attribute = levelPieGraph.getAttributeColumn().getSourceData().get(i).getValue().toUpperCase().replaceAll("\\s", "");
            String score = levelPieGraph.getScoreColumn().getSourceData().get(i).getValue();
            String assessor = levelPieGraph.getAssessorColumn().getSourceData().get(i).getValue();


            //Filter by assessor
            if (!levelPieGraph.getAssessorFilter().isEmpty()) {
                if (!assessorFilter.contains(assessor)) {
                    continue;
                }
            }

            if(process.equals("")) {
                continue;
            }
            processSet.add(process);

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

    /**
     * Method creates process level map, where each process level is calaculated by aggregating of assessor scores by defined function
     *
     * @return {process: level}
     */
    private LinkedHashMap<String, Integer> getLevelsByScore(LevelPieGraph levelPieGraph, MultiKeyMap valuesMap, List<String> processFilter) {
        LinkedHashMap<String, Integer> processLevelMap = new LinkedHashMap<>();
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
                            scoresList.add(applyScoreFunction(convertScoresToDoubles(assessorScoreList), levelPieGraph.getAggregateScoresFunction()));
                        } catch (JasperReportException e) {
                            throw new JasperReportException("Level pie graph id = " + levelPieGraph.getId() + " score column contains unknown value in: " +assessorScoreList, e);
                        }
                    }
                    //Get attribute score achieved as (sum of scores/count of scores)
                    calculateLevelCheckValue(scoresList.stream().mapToDouble(a -> a).sum() / scoresList.size(), i);
                }
                evaulateLevelCheckToLevel();
            }

            processLevelMap.put(process, levelAchieved);
        }
        return processLevelMap;
    }

    /**
     * Method creates process level map, where each process has level based on MIN/MAX level aggregation function
     *
     * @return {process: level}
     */
    private LinkedHashMap<String, Integer> getLevelsByLevel(LevelPieGraph levelPieGraph, MultiKeyMap valuesMap, List<String> processFilter, List<String> assessorFilter) {
        LinkedHashMap<String, Map<String, Integer>> processLevelMap = new LinkedHashMap<>();
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
                            throw new JasperReportException("Level pie graph score column contains unknown value: ", e);
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

        return mergeAssessors(processLevelMap, levelPieGraph.getAggregateScoresFunction());
    }

    /**
     * For each process merges all assessor levels achieved by aggregate function
     */
    private LinkedHashMap<String, Integer> mergeAssessors(LinkedHashMap<String, Map<String, Integer>> processLevelMap, ScoreFunction scoreFunction) {
        LinkedHashMap<String, Integer> updatedMap = new LinkedHashMap<>();
        for (String process : processLevelMap.keySet()) {
            List<Integer> levels = List.copyOf(processLevelMap.get(process).values());
            updatedMap.put(process, applyMinMaxFunction(levels, scoreFunction));
        }
        return updatedMap;
    }
}
