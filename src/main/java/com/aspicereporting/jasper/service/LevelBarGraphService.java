package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.enums.Orientation;
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
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
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
            for(var assessor : graphData.get(process).keySet()) {
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
        //Get all unique processes and levels
        List<String> processNames = sourceRepository.findDistinctColumnValuesForColumn(levelBarGraph.getProcessColumn().getId());
        List<String> assessorNames = sourceRepository.findDistinctColumnValuesForColumn(levelBarGraph.getAssessorColumn().getId());
        //Remove empty levels "" and processes ""
        processNames = processNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        assessorNames = assessorNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());

        //Apply process filter
        if (!levelBarGraph.getProcessFilter().isEmpty()) {
            processNames = processNames.stream().filter(process -> levelBarGraph.getProcessFilter().contains(process)).collect(Collectors.toList());
        }
        //Apply assessor filter
        if (!levelBarGraph.getAssessorFilter().isEmpty()) {
            assessorNames = assessorNames.stream().filter(assessor -> levelBarGraph.getAssessorFilter().contains(assessor)).collect(Collectors.toList());
        }

        //Sort alphabetically
        Collections.sort(processNames, new NaturalOrderComparator());

        //MultiKey map to store value for each process, level combination - {(process, attribute, assessor) : (criterion: [values])}
        MultiKeyMap valuesMap = new MultiKeyMap();
        for (int i = 0; i < levelBarGraph.getScoreColumn().getSourceData().size(); i++) {
            String processValue = levelBarGraph.getProcessColumn().getSourceData().get(i).getValue();
            String criterionValue = levelBarGraph.getCriterionColumn().getSourceData().get(i).getValue();
            String attributeValue = levelBarGraph.getAttributeColumn().getSourceData().get(i).getValue().toUpperCase().replaceAll("\\s", "");
            String scoreValue = levelBarGraph.getScoreColumn().getSourceData().get(i).getValue();
            String assessorValue = levelBarGraph.getAssessorColumn().getSourceData().get(i).getValue();

            //Filter by assessor and process
            if (!assessorNames.contains(assessorValue) || (!processNames.contains(processValue))) {
                continue;
            }

            MultiKey key = new MultiKey(processValue, attributeValue, assessorValue);
            if (valuesMap.containsKey(key)) {
                Map<String, ArrayList<String>> map = (Map<String, ArrayList<String>>) valuesMap.get(key);
                if (map.containsKey(criterionValue)) {
                    map.get(criterionValue).add(scoreValue);
                } else {
                    map.put(criterionValue, new ArrayList<>(Arrays.asList(scoreValue)));
                }
            } else {
                valuesMap.put(key, new HashMap(Map.of(criterionValue, new ArrayList(Arrays.asList(scoreValue)))));
            }
        }

        //Prepare default level for each process and assessor - {process, {assesor: level, assessor: level}, process,...}
        LinkedHashMap<String, Map<String, Integer>> processLevelMap = new LinkedHashMap<>();
        MultiKeyMap<String, Integer> processLevelAchievedMap = new MultiKeyMap();
        for (String process : processNames) {
            for (String assessor : assessorNames) {
                processLevelAchievedMap.put(process, assessor, 0);
                if (processLevelMap.containsKey(process)) {
                    Map<String,Integer> assessorMap = processLevelMap.get(process);
                    assessorMap.put(assessor, 0);
                    processLevelMap.put(process, assessorMap);
                } else {
                    processLevelMap.put(process, new HashMap<>(Map.of(assessor, 0)));
                }
            }
        }

        //Get level achieved for each process and acessor combination
        for (var assessor : assessorNames) {
            for (var process : processNames) {
                int levelAchieved = 0;
                boolean previousLevelAchieved = true;

                for (int i = 1; i <= 5; i++) {
                    double levelValue = 0;
                    //If previous level is not fully achieved move to another process
                    if (!previousLevelAchieved) {
                        break;
                    }

                    for (String attribute : processAttributesMap.get(i)) {
                        double levelCheckValue = 0;

                        MultiKey multikey = new MultiKey(process, attribute, assessor);
                        //Process does not have this attribute defined we dont have to increase level
                        if (!valuesMap.containsKey(multikey)) {
                            break;
                        }

                        //Get all criterion scores for (process, attribute, assessor) key and apply score function on them
                        Map<String, ArrayList<String>> criterionScoreMap = (Map<String, ArrayList<String>>) valuesMap.get(multikey);
                        for(String criterionKey : criterionScoreMap.keySet()) {
                            List<String> scoresList = criterionScoreMap.get(criterionKey);
                            List<Double> scoresListDouble = new ArrayList<>();
                            for(int j =0; j<scoresList.size(); j++) {
                                String score = scoresList.get(j);
                                try {
                                    Double doubleScore = getValueForScore(score);
                                    scoresListDouble.add(doubleScore);
                                } catch (Exception e) {
                                    throw new JasperReportException("Level bar graph score column contains unknown value: " + score, e);
                                }
                            }
                            levelCheckValue += applyScoreFunction(scoresListDouble, levelBarGraph.getScoreFunction());
                        }
                        //Get average score achieved for this attribute
                        levelCheckValue = levelCheckValue / criterionScoreMap.size();

                        //Set score achieved for this attribute
                        if (levelCheckValue > 0.85) {
                            if (i == 1) {
                                levelValue += 2;
                            } else {
                                levelValue += 1;
                            }
                        } else if (levelCheckValue > 0.5) {
                            if (i == 1) {
                                levelValue += 1;
                            } else {
                                levelValue += 0.5;
                            }
                        }
                    }

                    //0 - not achieved, 1 - all defined attributes are largely achieved, 2- all are fully
                    if (levelValue == 2) {
                        levelAchieved += 1;
                    } else {
                        //All attributes are at least largely achieved
                        if (levelValue >= 1) {
                            levelAchieved += 1;
                        }
                        //We need to have all attributes fully to continue
                        previousLevelAchieved = false;
                    }
                }

                Map<String, Integer> assessorLevelMap = processLevelMap.get(process);
                assessorLevelMap.put(assessor, levelAchieved);
                processLevelMap.put(process,assessorLevelMap);
            }
        }
        return processLevelMap;
    }
}
