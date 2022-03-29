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
        //Apply assessor filter assesor list
        if (!levelBarGraph.getAssessorFilter().isEmpty()) {
            assessorFilter = assessorFilter.stream().filter(assessor -> levelBarGraph.getAssessorFilter().contains(assessor)).collect(Collectors.toList());
        }

        //Sort alphabetically
        Collections.sort(processFilter, new NaturalOrderComparator());

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

            MultiKey key = new MultiKey(process, attribute, assessor);
            if (valuesMap.containsKey(key)) {
                Map<String, String> map = (Map<String, String>) valuesMap.get(key);
                map.put(criterion, score);
            } else {
                valuesMap.put(key, new HashMap(Map.of(criterion, score)));
            }
        }

        //Prepare default level for each process and assessor - {process, {assesor: level, assessor: level}, process,...}
        LinkedHashMap<String, Map<String, Integer>> processLevelMap = new LinkedHashMap<>();
        MultiKeyMap<String, Integer> processLevelAchievedMap = new MultiKeyMap();
        for (String process : processFilter) {
            for (String assessor : assessorFilter) {
                processLevelAchievedMap.put(process, assessor, 0);
                if (processLevelMap.containsKey(process)) {
                    Map<String, Integer> assessorMap = processLevelMap.get(process);
                    assessorMap.put(assessor, 0);
                    processLevelMap.put(process, assessorMap);
                } else {
                    processLevelMap.put(process, new HashMap<>(Map.of(assessor, 0)));
                }
            }
        }

        //Get level achieved for each process and assessor combination
        for (var assessor : assessorFilter) {
            for (var process : processFilter) {
                resetVariables();
                for (int i = 1; i <= 5; i++) {
                    //If previous level is not fully achieved move to another process
                    if (!previousLevelAchieved) {
                        break;
                    }
                    resetCheckVariable();

                    for (String attribute : processAttributesMap.get(i)) {
                        double scoreAchieved = 0;

                        MultiKey multikey = new MultiKey(process, attribute, assessor);
                        //Process does not have this attribute defined we dont have to increase level
                        if (!valuesMap.containsKey(multikey)) {
                            break;
                        }

                        //Get all criterion scores for (process, attribute, assessor) key and apply score function on them
                        Map<String, String> criterionScoreMap = (Map<String, String>) valuesMap.get(multikey);
                        List<String> stringScoreList = new ArrayList<>();
                        for (String criterionKey : criterionScoreMap.keySet()) {
                            stringScoreList.add(criterionScoreMap.get(criterionKey));
                        }
                        List<Double> scoresList;
                        try {
                            scoresList = convertScoresToDoubles(stringScoreList);
                        } catch (JasperReportException e) {
                            throw new JasperReportException("Level bar graph score column contains unknown value: ", e);
                        }

                        //Get attribute score achieved as (sum of scores/count of scores)
                        scoreAchieved = scoresList.stream().mapToDouble(a -> a).sum() / scoresList.size();
                        calculateLevelCheckValue(scoreAchieved, i);
                    }
                    evaulateLevelCheckToLevel();
                }

                Map<String, Integer> assessorLevelMap = processLevelMap.get(process);
                assessorLevelMap.put(assessor, levelAchieved);
                processLevelMap.put(process, assessorLevelMap);
            }
        }

        //Merge levels for assessors with defined merge function
        LinkedHashMap<String, Map<String, Integer>> resultMap = new LinkedHashMap<>();
        if (!levelBarGraph.getScoreFunction().equals(ScoreFunction.NONE)) {
            String seriesName = levelBarGraph.getScoreFunction().name() + " levels for merged assessors";
            for(String process : processLevelMap.keySet()) {
                resultMap.put(process, new HashMap<>());
                List<Integer> processLevels = new ArrayList<>();
                for(String assessor: processLevelMap.get(process).keySet()) {
                    processLevels.add(processLevelMap.get(process).get(assessor));
                }
                Integer value;
                switch(levelBarGraph.getScoreFunction()) {
                    case MIN:
                        value = Collections.min(processLevels);
                        break;
                    default:
                        value = Collections.max(processLevels);
                        break;
                }
                resultMap.get(process).put(seriesName, value);
            }
        } else {
            resultMap = processLevelMap;
        }

        return resultMap;
    }
}
