package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.enums.Orientation;
import com.aspicereporting.entity.items.CapabilityBarGraph;
import com.aspicereporting.exception.InvalidDataException;
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
public class CapabilityBarGraphService extends BaseChartService {
    @Autowired
    SourceRepository sourceRepository;
    @Autowired
    SourceColumnRepository sourceColumnRepository;

    public JRDesignImage createElement(JasperDesign jasperDesign, CapabilityBarGraph capabilityBarGraph, Integer counter, Map<String, Object> parameters) throws JRException {
        LinkedHashMap<String, Map<String, Integer>> graphData = getData(capabilityBarGraph);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int maxLevel = 0;
        for (var process : graphData.keySet()) {
            for(var assessor : graphData.get(process).keySet()) {
                Integer level = graphData.get(process).get(assessor);
                if (level > maxLevel) {
                    maxLevel = level;
                }
                dataset.addValue(level, assessor, process);
            }
        }


        final JFreeChart chart = ChartFactory.createBarChart(
                "",                                   // chart title
                "Process",                  // domain axis label
                "Level",                     // range axis label
                dataset,                            // data
                capabilityBarGraph.getOrientation().equals(Orientation.VERTICAL) ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL,           // the plot orientation
                true,                        // legend
                false,                        // tooltips
                false                        // urls
        );
        this.applyBarGraphTheme(chart);

        //Rotate x axis names to save space
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setUpperBound(maxLevel < 5 ? maxLevel + 1 : maxLevel);
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        if (capabilityBarGraph.getOrientation().equals(Orientation.HORIZONTAL)) {
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
        imageElement.setX(capabilityBarGraph.getX());
        imageElement.setY(capabilityBarGraph.getY());
        imageElement.setWidth(capabilityBarGraph.getWidth());
        imageElement.setHeight(capabilityBarGraph.getHeight());
        imageElement.setPositionType(PositionTypeEnum.FLOAT);
        imageElement.setScaleImage(ScaleImageEnum.FILL_FRAME);
        imageElement.setLazy(true);
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("$P{" + CHART + counter + "}");
        expression.setValueClass(JRRenderable.class);
        imageElement.setExpression(expression);

        return imageElement;
    }

    public LinkedHashMap<String, Map<String, Integer>> getData(CapabilityBarGraph capabilityBarGraph) {
        //Get all unique processes and levels
        List<String> processNames = sourceRepository.findDistinctColumnValuesForColumn(capabilityBarGraph.getProcessColumn().getId());
        List<String> levelNames = sourceRepository.findDistinctColumnValuesForColumn(capabilityBarGraph.getLevelColumn().getId());
        List<String> assessorNames = sourceRepository.findDistinctColumnValuesForColumn(capabilityBarGraph.getAssessorColumn().getId());
        //Remove empty levels "" and processes ""
        levelNames = levelNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        processNames = processNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        assessorNames = assessorNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());

        //Apply process filter
        if (!capabilityBarGraph.getProcessFilter().isEmpty()) {
            processNames = processNames.stream().filter(process -> capabilityBarGraph.getProcessFilter().contains(process)).collect(Collectors.toList());
        }
        //Apply assessor filter
        if (!capabilityBarGraph.getAssessorFilter().isEmpty()) {
            assessorNames = assessorNames.stream().filter(assessor -> capabilityBarGraph.getAssessorFilter().contains(assessor)).collect(Collectors.toList());
        }

        if (levelNames.size() > 5) {
            throw new InvalidDataException("Source: \"" + capabilityBarGraph.getSource().getSourceName() + "\" has more than 5 capability levels defined");
        }

        //Sort alphabetically
        Collections.sort(levelNames, new NaturalOrderComparator());
        Collections.sort(processNames, new NaturalOrderComparator());

        //MultiKey map to store value for each process, level combination - {(process, level) : (atribute: [value])}
        MultiKeyMap valuesMap = new MultiKeyMap();
        for (int i = 0; i < capabilityBarGraph.getScoreColumn().getSourceData().size(); i++) {
            String processValue = capabilityBarGraph.getProcessColumn().getSourceData().get(i).getValue();
            String levelValue = capabilityBarGraph.getLevelColumn().getSourceData().get(i).getValue();
            String attributeValue = capabilityBarGraph.getAttributeColumn().getSourceData().get(i).getValue().toUpperCase().replaceAll("\\s", "");
            String scoreValue = capabilityBarGraph.getScoreColumn().getSourceData().get(i).getValue();
            String assessorValue = capabilityBarGraph.getAssessorColumn().getSourceData().get(i).getValue();

            //Filter by assessor and process
            if (!assessorNames.contains(assessorValue) || (!processNames.contains(processValue))) {
                continue;
            }

            MultiKey key = new MultiKey(processValue, levelValue, assessorValue);
            if (valuesMap.containsKey(key)) {
                Map<String, ArrayList<String>> map = (Map<String, ArrayList<String>>) valuesMap.get(key);
                if (map.containsKey(attributeValue)) {
                    map.get(attributeValue).add(scoreValue);
                } else {
                    map.put(attributeValue, new ArrayList<>(Arrays.asList(scoreValue)));
                }
            } else {
                valuesMap.put(key, new HashMap(Map.of(attributeValue, new ArrayList(Arrays.asList(scoreValue)))));
            }
        }

        //Prepare default level for each process and assessor
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

        for (var assessor : assessorNames) {
            for (var process : processNames) {
                int level = 0;
                boolean isPreviousLevelAchieved = true;

                for (int i = 0; i < levelNames.size(); i++) {
                    double levelValue = 0;
                    //If previous level is not fully achieved move to another process
                    if (!isPreviousLevelAchieved) {
                        break;
                    }

                    MultiKey multikey = new MultiKey(process, levelNames.get(i), assessor);
                    //Process does not have this level defined - we don't have to increment level achieved
                    if (!valuesMap.containsKey(multikey)) {
                        break;
                    }
                    //Get all scores for (process, level, assessor) key
                    Map<String, ArrayList<String>> attributesScoreMap = (Map<String, ArrayList<String>>) valuesMap.get(multikey);
                    for (String attribute : processAttributesMap.get(i + 1)) {
                        double avgScore = 0;
                        if (attributesScoreMap.containsKey(attribute)) {
                            //Create sum of all scores for attribute
                            for (String s : attributesScoreMap.get(attribute)) {
                                double score;
                                if (scoreToValueMap.containsKey(s)) {
                                    score = scoreToValueMap.get(s);
                                } else {
                                    try {
                                        score = Double.parseDouble(s);
                                    } catch (Exception e) {
                                        throw new JasperReportException("Capability graph score column contains non numeric or unknown value: " + s, e);
                                    }
                                }
                                avgScore += score;
                            }
                        }
                        //Get average score achieved for this attribute
                        avgScore = avgScore / attributesScoreMap.get(attribute).size();

                        //Set score achieved for this attribute
                        if (avgScore > 0.85) {
                            if (i == 0) {
                                levelValue += 2;
                            } else {
                                levelValue += 1;
                            }
                        } else if (avgScore > 0.5) {
                            if (i == 0) {
                                levelValue += 1;
                            } else {
                                levelValue += 0.5;
                            }
                        }
                    }

                    //0 - not achieved, 1 - all defined attributes are largely achieved, 2- all are fully
                    if (levelValue == 2) {
                        level += 1;
                    } else {
                        //All attributes are at least largely achieved
                        if (levelValue >= 1) {
                            level += 1;
                        }
                        //We need to have all attributes fully to continue
                        isPreviousLevelAchieved = false;
                    }
                }

                Map<String, Integer> assessorLevelMap = processLevelMap.get(process);
                assessorLevelMap.put(assessor, level);
                processLevelMap.put(process,assessorLevelMap);
            }
        }
        return processLevelMap;
    }
}
