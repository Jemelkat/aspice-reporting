package com.aspicereporting.jasper.service;

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
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
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

    public JRDesignImage createElement(JasperDesign jasperDesign, LevelPieGraph levelPieGraph, Integer counter, Map<String, Object> parameters) throws JRException {
        LinkedHashMap<String, Integer> graphData = getData(levelPieGraph);

        DefaultPieDataset dataset = new DefaultPieDataset();
        for (var level : graphData.keySet()) {
            Integer count = graphData.get(level);
            dataset.setValue(level, count);
        }

        final JFreeChart chart = ChartFactory.createPieChart(
                null,
                dataset,
                true,
                true,
                false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setOutlinePaint(null);
        plot.setShadowPaint(null);
        //Label
        PieSectionLabelGenerator gen = new StandardPieSectionLabelGenerator(
                "{1} ({2})", NumberFormat.getInstance(), NumberFormat.getPercentInstance());
        plot.setSimpleLabels(true);
        plot.setLabelGenerator(gen);
        plot.setLabelBackgroundPaint(Color.white);
        plot.setLabelShadowPaint(null);
        plot.setLabelOutlinePaint(null);
        //TODO SCALE TEXT
        //plot.setLabelFont(new Font("test", Font.PLAIN, 5));
        //Legend
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

    public LinkedHashMap<String, Integer> getData(LevelPieGraph levelPieGraph) {
        LinkedHashMap<String, Integer> levelCounts = new LinkedHashMap<>();
        levelCounts.put("0", 0);
        levelCounts.put("1", 0);
        levelCounts.put("2", 0);
        levelCounts.put("3", 0);
        levelCounts.put("4", 0);
        levelCounts.put("5", 0);

        //Get all unique processes and levels
        List<String> processNames = sourceRepository.findDistinctColumnValuesForColumn(levelPieGraph.getProcessColumn().getId());
        List<String> assessorNames = sourceRepository.findDistinctColumnValuesForColumn(levelPieGraph.getAssessorColumn().getId());
        //Remove empty assessors "" and processes ""
        processNames = processNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        assessorNames = assessorNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());

        //Apply assessor filter
        if (levelPieGraph.getAssessorFilter() != null && !levelPieGraph.getAssessorFilter().isBlank()) {
            assessorNames = assessorNames.stream().filter(assessor -> assessor.equals(levelPieGraph.getAssessorFilter())).collect(Collectors.toList());
        } else {
            if (assessorNames.isEmpty()) {
                throw new InvalidDataException("There are no assessors defined in column " + levelPieGraph.getAssessorColumn().getColumnName());
            } else {
                //If no assessor is defined - we will use first assessor found
                assessorNames.subList(1, assessorNames.size()).clear();
            }
        }
        String assessor = assessorNames.get(0);

                //Sort alphabetically
        Collections.sort(processNames, new NaturalOrderComparator());

        //MultiKey map to store value for each process, level combination - {(process, level) : (atribute: [value])}
        MultiKeyMap valuesMap = new MultiKeyMap();
        for (int i = 0; i < levelPieGraph.getScoreColumn().getSourceData().size(); i++) {
            String processValue = levelPieGraph.getProcessColumn().getSourceData().get(i).getValue();
            String criterionValue = levelPieGraph.getCriterionColumn().getSourceData().get(i).getValue();
            String attributeValue = levelPieGraph.getAttributeColumn().getSourceData().get(i).getValue().toUpperCase().replaceAll("\\s", "");
            String scoreValue = levelPieGraph.getScoreColumn().getSourceData().get(i).getValue();
            String assessorValue = levelPieGraph.getAssessorColumn().getSourceData().get(i).getValue();

            //Filter by assessor
            if (!assessorValue.equals(assessor)) {
                continue;
            }

            MultiKey key = new MultiKey(processValue, attributeValue);
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

        //Prepare default level for each process
        HashMap<String, Double> processLevelAchievedMap = new HashMap<>();
        for (String process : processNames) {
            processLevelAchievedMap.put(process, 0D);
        }

        for (var process : processNames) {
            Integer level = 0;
            boolean isPreviousLevelAchieved = true;

            for (int i = 1; i < 5; i++) {
                //If previous level is not fully achieved move to another process
                if (!isPreviousLevelAchieved) {
                    break;
                }

                for (String attribute : processAttributesMap.get(i)) {
                    double finalScore = 0;
                    MultiKey multikey = new MultiKey(process, attribute);
                    //Process does not have this level defined - we don't have to increment level achieved
                    if (!valuesMap.containsKey(multikey)) {
                        break;
                    }
                    //Get all scores for (process, atrribute) key
                    Map<String, ArrayList<String>> criterionScoreMap = (Map<String, ArrayList<String>>) valuesMap.get(multikey);
                    for(String criterionKey : criterionScoreMap.keySet()) {
                        List<String> scoresList = criterionScoreMap.get(criterionKey);
                        List<Double> scoresListDouble = new ArrayList<>();
                        for(int j =0; j<scoresList.size(); j++) {
                            String score = scoresList.get(j);
                            if (scoreToValueMap.containsKey(score)) {
                                scoresListDouble.add(scoreToValueMap.get(score));
                            } else {
                                try {
                                    scoresListDouble.add(Double.parseDouble(score));
                                } catch (Exception e) {
                                    throw new JasperReportException("Level pie graph score column contains unknown value: " + score, e);
                                }
                            }
                        }

                        switch(levelPieGraph.getScoreFunction()) {
                            case MIN:
                                finalScore += Collections.min(scoresListDouble);
                                break;
                            case MAX:
                                finalScore += Collections.max(scoresListDouble);
                                break;
                            case AVG:
                                finalScore += scoresListDouble.stream().mapToDouble(s -> s).average().getAsDouble();
                                break;
                            default:
                                throw new JasperReportException("Level pie contains unknown score function: " + levelPieGraph.getScoreFunction().toString());
                        }
                    }

                    //Get average score achieved for this attribute
                    finalScore = finalScore / criterionScoreMap.size();

                    //Set score achieved for this attribute
                    //
                    if (finalScore > 0.85) {
                        if (i == 1) {
                            processLevelAchievedMap.put(process, processLevelAchievedMap.get(process) + 2);
                        } else {
                            processLevelAchievedMap.put(process, processLevelAchievedMap.get(process) + 1);
                        }
                    } else if (finalScore > 0.5) {
                        if (i == 1) {
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
                //Reset level achieved check value
                processLevelAchievedMap.put(process, 0D);
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
