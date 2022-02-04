package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.items.CapabilityBarGraph;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.repository.SourceColumnRepository;
import com.aspicereporting.repository.SourceRepository;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRenderable;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.renderers.JCommonDrawableRendererImpl;
import net.sf.jasperreports.renderers.Renderable;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.*;
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

    public JRDesignImage createElement(JasperDesign jasperDesign, CapabilityBarGraph capabilityBarGraph, Integer counter, Map<String, Object> parameters) {
        //Get all unique processes and levels
        List<String> processNames = sourceRepository.findDistinctByColumnId(capabilityBarGraph.getProcessColumn().getId());
        List<String> levelNames = sourceRepository.findDistinctByColumnId(capabilityBarGraph.getLevelColumn().getId());
        //Remove empty levels "" and processes ""
        levelNames = levelNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        processNames = processNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());

        if (levelNames.size() > 5) {
            throw new InvalidDataException("Source: \"" + capabilityBarGraph.getSource().getSourceName() + "\" has more than 5 capability levels defined");
        }

        //Sort alphabetically
        Collections.sort(levelNames);
        Collections.sort(processNames);

        //MultiKey map to store value for each process, level combination - {(process, level) : [values]}
        MultiKeyMap valuesMap = new MultiKeyMap();
        for (int i = 0; i < capabilityBarGraph.getScoreColumn().getSourceData().size(); i++) {
            String processValue = capabilityBarGraph.getProcessColumn().getSourceData().get(i).getValue();
            String levelValue = capabilityBarGraph.getLevelColumn().getSourceData().get(i).getValue();
            String attributeValue = capabilityBarGraph.getAttributeColumn().getSourceData().get(i).getValue().toUpperCase().replaceAll("\\s","");
            String scoreValue = capabilityBarGraph.getScoreColumn().getSourceData().get(i).getValue();

            MultiKey key = new MultiKey(processValue, levelValue);
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


        HashMap<Integer, ArrayList<String>> processAttributesMap = new HashMap<>();
        processAttributesMap.put(1, new ArrayList<>(Arrays.asList("PA1.1")));
        processAttributesMap.put(2, new ArrayList<>(Arrays.asList("PA2.1", "PA2.2")));
        processAttributesMap.put(3, new ArrayList<>(Arrays.asList("PA3.1", "PA3.2")));
        processAttributesMap.put(4, new ArrayList<>(Arrays.asList("PA4.1", "PA4.2")));
        processAttributesMap.put(5, new ArrayList<>(Arrays.asList("PA5.1", "PA5.2")));

        //0 - not achieved, 1 - both are largely achieved, 2- all are fully
        HashMap<String, Double> processLevelAchievedMap = new HashMap<>();
        for (String process : processNames) {
            processLevelAchievedMap.put(process, 0D);
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (var process : processNames) {
            int level = 0;
            boolean isPreviousLevelAchieved = true;

            //For each level defined
            for (int i = 0; i < levelNames.size(); i++) {
                if (!isPreviousLevelAchieved) {
                    break;
                }

                MultiKey multikey = new MultiKey(process, levelNames.get(i));
                //Process does not have this level defined - we don't have to increment level achieved
                if (!valuesMap.containsKey(multikey)) {
                    break;
                }
                //Get atribute scores for (process, level) key
                Map<String, ArrayList<String>> attributesScoreMap = (Map<String, ArrayList<String>>) valuesMap.get(multikey);
                for (String attribute : processAttributesMap.get(i + 1)) {
                    double avgScore = 0;
                    if (attributesScoreMap.containsKey(attribute)) {
                        for (String s : attributesScoreMap.get(attribute)) {
                            double valueOf = Double.parseDouble(s);
                            avgScore += valueOf;
                        }
                    }
                    //Get average score achieved
                    avgScore = avgScore / attributesScoreMap.get(attribute).size();

                    //Set score achieved for this process
                    if (avgScore > 0.85) {
                        if (i == 0) {
                            processLevelAchievedMap.put(process, processLevelAchievedMap.get(process) + 2);
                        } else {
                            processLevelAchievedMap.put(process, processLevelAchievedMap.get(process) + 1);
                        }
                    } else if (avgScore > 0.5) {
                        if (i == 0) {
                            processLevelAchievedMap.put(process, processLevelAchievedMap.get(process) + 1);
                        } else {
                            processLevelAchievedMap.put(process, processLevelAchievedMap.get(process) + 0.5);
                        }
                    }
                }

                Double finalAttributesScore = processLevelAchievedMap.get(process);
                //All attributes are fully achieved
                if (finalAttributesScore == 2) {
                    level += 1;
                } else {
                    //All attributes are at least largely achieved
                    if (finalAttributesScore >= 1) {
                        level += 1;
                    }
                    //We need to have all attributes fully to continue
                    isPreviousLevelAchieved = false;
                }
            }
            dataset.addValue(level, "", process);
        }


        final JFreeChart chart = ChartFactory.createBarChart(
                "",                                   // chart title
                "Process",                  // domain axis label
                "Capability level",                     // range axis label
                dataset,                     // data
                PlotOrientation.VERTICAL,    // the plot orientation
                false,                        // legend
                false,                        // tooltips
                false                        // urls
        );
        this.applyBarGraphTheme(chart);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setUpperBound(5);
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        CategoryAxis categoryAxis = plot.getDomainAxis();
        categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

        parameters.put("Chart", new JCommonDrawableRendererImpl(chart));

        JRDesignParameter parameter = new JRDesignParameter();
        parameter.setValueClass(Renderable.class);
        parameter.setName("Chart");
        try {
            jasperDesign.addParameter(parameter);
        } catch (JRException e) {
            e.printStackTrace();
        }


        JRDesignImage imageElement = new JRDesignImage(jasperDesign);
        imageElement.setX(capabilityBarGraph.getX());
        imageElement.setY(capabilityBarGraph.getY());
        imageElement.setWidth(capabilityBarGraph.getWidth());
        imageElement.setHeight(capabilityBarGraph.getHeight());
        imageElement.setPositionType(PositionTypeEnum.FLOAT);
        imageElement.setScaleImage(ScaleImageEnum.FILL_FRAME);
        imageElement.setLazy(true);
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("$P{Chart}");
        expression.setValueClass(JRRenderable.class);
        imageElement.setExpression(expression);

        return imageElement;
    }
}
