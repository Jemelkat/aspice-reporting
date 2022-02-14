package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.items.CapabilityBarGraph;
import com.aspicereporting.entity.items.LevelPieGraph;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.RectangleInsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LevelPieGraphService {
    @Autowired
    SourceRepository sourceRepository;

    Map<String, Double> scoreToValueMap = Map.ofEntries(
            new AbstractMap.SimpleEntry<>("N", 0D),
            new AbstractMap.SimpleEntry<>("P", 0.33D),
            new AbstractMap.SimpleEntry<>("L", 0.66D),
            new AbstractMap.SimpleEntry<>("F", 1D)
    );

    Map<Integer, ArrayList<String>> processAttributesMap = Map.ofEntries(
            new AbstractMap.SimpleEntry<>(1, new ArrayList<>(Arrays.asList("PA1.1"))),
            new AbstractMap.SimpleEntry<>(2, new ArrayList<>(Arrays.asList("PA2.1", "PA2.2"))),
            new AbstractMap.SimpleEntry<>(3, new ArrayList<>(Arrays.asList("PA3.1", "PA3.2"))),
            new AbstractMap.SimpleEntry<>(4, new ArrayList<>(Arrays.asList("PA4.1", "PA4.2"))),
            new AbstractMap.SimpleEntry<>(5, new ArrayList<>(Arrays.asList("PA5.1", "PA5.2")))
    );


    public JRDesignImage createElement(JasperDesign jasperDesign, LevelPieGraph levelPieGraph, Integer counter, Map<String, Object> parameters) throws JRException {
        LinkedHashMap<String, Integer> graphData = getData(levelPieGraph);

        DefaultPieDataset dataset = new DefaultPieDataset();
        for (var level : graphData.keySet()) {
            Integer count = graphData.get(level);
            dataset.setValue(level, count);
        }

        final JFreeChart chart = ChartFactory.createPieChart(
                "Pie Chart",
                dataset,
                true,
                true,
                false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSimpleLabels(true);

        PieSectionLabelGenerator gen = new StandardPieSectionLabelGenerator(
                "{1} ({2})", NumberFormat.getInstance(), NumberFormat.getPercentInstance());
        plot.setLabelGenerator(gen);

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

    public LinkedHashMap<String, Integer> getData(LevelPieGraph levelPieGraph) {
        LinkedHashMap<String, Integer> levelCounts = new LinkedHashMap<>();
        levelCounts.put("0", 0);
        levelCounts.put("1", 0);
        levelCounts.put("2", 0);
        levelCounts.put("3", 0);
        levelCounts.put("4", 0);
        levelCounts.put("5", 0);

        //Get all unique processes and levels
        List<String> processNames = sourceRepository.findDistinctByColumnId(levelPieGraph.getProcessColumn().getId());
        List<String> levelNames = sourceRepository.findDistinctByColumnId(levelPieGraph.getLevelColumn().getId());
        //Remove empty levels "" and processes ""
        levelNames = levelNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        processNames = processNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());

        if (levelNames.size() > 5) {
            throw new InvalidDataException("Source: \"" + levelPieGraph.getSource().getSourceName() + "\" has more than 5 capability levels defined");
        }

        //Sort alphabetically
        Collections.sort(levelNames, new NaturalOrderComparator());
        Collections.sort(processNames, new NaturalOrderComparator());

        //MultiKey map to store value for each process, level combination - {(process, level) : (atribute: [value])}
        MultiKeyMap valuesMap = new MultiKeyMap();
        for (int i = 0; i < levelPieGraph.getScoreColumn().getSourceData().size(); i++) {
            String processValue = levelPieGraph.getProcessColumn().getSourceData().get(i).getValue();
            String levelValue = levelPieGraph.getLevelColumn().getSourceData().get(i).getValue();
            String attributeValue = levelPieGraph.getAttributeColumn().getSourceData().get(i).getValue().toUpperCase().replaceAll("\\s", "");
            String scoreValue = levelPieGraph.getScoreColumn().getSourceData().get(i).getValue();

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

        //Prepare default level for each process
        HashMap<String, Double> processLevelAchievedMap = new HashMap<>();
        for (String process : processNames) {
            processLevelAchievedMap.put(process, 0D);
        }

        for (var process : processNames) {
            Integer level = 0;
            boolean isPreviousLevelAchieved = true;

            for (int i = 0; i < levelNames.size(); i++) {
                //If previous level is not fully achieved move to another process
                if (!isPreviousLevelAchieved) {
                    break;
                }

                MultiKey multikey = new MultiKey(process, levelNames.get(i));
                //Process does not have this level defined - we don't have to increment level achieved
                if (!valuesMap.containsKey(multikey)) {
                    break;
                }
                //Get all scores for (process, level) key
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
                    //
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
                //0 - not achieved, 1 - all defined attributes are largely achieved, 2- all are fully
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
            if (level > 5) {
                throw new InvalidDataException("Pie graph has maximum level 5. Please use check your source.");
            }

            levelCounts.put(level.toString(), levelCounts.get(level.toString()) + 1);
        }

        for (var key : new LinkedHashMap<>(levelCounts).keySet()) {
            Integer count = levelCounts.get(key);
            if (count == 0) {
                levelCounts.remove(key);
            } else {
                levelCounts.remove(key);
                levelCounts.put("Level " + key, count);
            }
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(levelCounts.entrySet());
        Collections.sort(entries, Map.Entry.comparingByValue(Comparator.reverseOrder()));

        levelCounts.clear();
        for (Map.Entry<String, Integer> e : entries) {
            levelCounts.put(e.getKey(), e.getValue());
        }

        return levelCounts;
    }
}
