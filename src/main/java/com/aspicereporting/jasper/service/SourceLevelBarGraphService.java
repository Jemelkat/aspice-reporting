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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SourceLevelBarGraphService extends BaseChartService {
    @Autowired
    SourceRepository sourceRepository;

    public JRDesignImage createElement(JasperDesign jasperDesign, SourceLevelBarGraph sourceLevelBarGraph, Integer counter, Map<String, Object> parameters) throws JRException {
        LinkedHashMap<String, Map<String, Integer>> graphData = getData(sourceLevelBarGraph);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int maxLevel = 0;
        for (var process : graphData.keySet()) {
            for (var assessor : graphData.get(process).keySet()) {
                Integer level = graphData.get(process).get(assessor);
                if (level > maxLevel) {
                    maxLevel = level;
                }
                dataset.addValue(level, process, assessor);
            }
        }


        final JFreeChart chart = ChartFactory.createBarChart(
                "",                                   // chart title
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
        //Result data in format {sourceName: {process1: level, process2: level}, sourceName2: ...}
        LinkedHashMap<String, Map<String, Integer>> dataMap = new LinkedHashMap<>();

        //Will store all processes and set level to 0 to sources that did not have this process defined
        Set<String> allProcessSet = new LinkedHashSet<>();
        //Get all process filters
        List<String> processNames = sourceLevelBarGraph.getProcessFilter();
        Collections.sort(processNames, new NaturalOrderComparator());
        allProcessSet.addAll(processNames);

        //Precompile regex pattern or use first assessor found as assessor
        Pattern assessorPattern = Pattern.compile(sourceLevelBarGraph.getAssessorFilter() != null ? sourceLevelBarGraph.getAssessorFilter().trim() : "");
        Matcher assessorMatcher = assessorPattern.matcher("");
        String firstAssessor = null;

        for (Source source : sourceLevelBarGraph.getSources()) {
            SourceColumn assessorColumn = getSourceColumnByName(source, sourceLevelBarGraph.getAssessorColumn());
            SourceColumn processColumn = getSourceColumnByName(source, sourceLevelBarGraph.getProcessColumn());
            SourceColumn criterionColumn = getSourceColumnByName(source, sourceLevelBarGraph.getCriterionColumn());
            SourceColumn attributeColumn = getSourceColumnByName(source, sourceLevelBarGraph.getAttributeColumn());
            SourceColumn scoreColumn = getSourceColumnByName(source, sourceLevelBarGraph.getScoreColumn());

            //Get all process names sorted - only if not defined in filter
            if (sourceLevelBarGraph.getProcessFilter().isEmpty()) {
                List<String> currentProcessNames = sourceRepository.findDistinctColumnValuesForColumn(processColumn.getId());
                allProcessSet.addAll(currentProcessNames);
                processNames.addAll(currentProcessNames);
                Collections.sort(processNames, new NaturalOrderComparator());
            }

            //Get first assessor if assessor filter is not defined
            if (firstAssessor == null && (sourceLevelBarGraph.getAssessorFilter() == null || sourceLevelBarGraph.getAssessorFilter().trim().isEmpty())) {
                List<String> assessorNames = sourceRepository.findDistinctColumnValuesForColumn(assessorColumn.getId());
                if (!assessorNames.isEmpty()) {
                    firstAssessor = assessorNames.get(0).trim();
                } else {
                    throw new InvalidDataException("Source: " + source.getSourceName() + " has no assessors defined.");
                }
            }

            //Get all related data to map for easier lookup - {(process, attribute, criterion) : [values]}
            MultiKeyMap sourceDataMap = new MultiKeyMap();
            for (int i = 0; i < scoreColumn.getSourceData().size(); i++) {
                String process = processColumn.getSourceData().get(i).getValue();
                String attribute = attributeColumn.getSourceData().get(i).getValue();
                String criterion = criterionColumn.getSourceData().get(i).getValue();
                String score = scoreColumn.getSourceData().get(i).getValue();
                String assessor = assessorColumn.getSourceData().get(i).getValue();

                //Filter by process
                if(!processNames.contains(process)) {
                    continue;
                }

                //Filter by assessor filter or first assessor found in first source file
                if (firstAssessor == null) {
                    assessorMatcher.reset(assessor);
                    if (!assessorMatcher.matches()) {
                        continue;
                    }
                } else {
                    if (!firstAssessor.equals(assessor)) {
                        continue;
                    }
                }

                MultiKey key = new MultiKey(process, attribute);
                if (sourceDataMap.containsKey(key)) {
                    Map<String, ArrayList<String>> map = (Map<String, ArrayList<String>>) sourceDataMap.get(key);
                    if (map.containsKey(criterion)) {
                        map.get(criterion).add(score);
                    } else {
                        map.put(criterion, new ArrayList<>(Arrays.asList(score)));
                    }
                } else {
                    sourceDataMap.put(key, new HashMap(Map.of(criterion, new ArrayList(Arrays.asList(score)))));
                }
            }

            LinkedHashMap<String, Integer> levelMap = new LinkedHashMap<>();
            for (String process : processNames) {

                int levelAchieved = 0;
                boolean previousLevelAchieved = true;
                for (int i = 1; i <= 5; i++) {
                    double levelCheckValue = 0;
                    if (!previousLevelAchieved) {
                        break;
                    }
                    for (String attribute : processAttributesMap.get(i)) {
                        double scoreAchieved = 0;
                        MultiKey multikey = new MultiKey(process, attribute);
                        //Process does not have this attribute defined we dont have to increase level
                        if (!sourceDataMap.containsKey(multikey)) {
                            break;
                        }

                        //Get all criterion scores for (process, attribute, assessor) key and apply score function on them
                        Map<String, ArrayList<String>> criterionScoreMap = (Map<String, ArrayList<String>>) sourceDataMap.get(multikey);
                        for (String criterionKey : criterionScoreMap.keySet()) {
                            List<String> scoresList = criterionScoreMap.get(criterionKey);
                            List<Double> scoresListDouble = new ArrayList<>();
                            for (int j = 0; j < scoresList.size(); j++) {
                                String score = scoresList.get(j);
                                try {
                                    Double doubleScore = getValueForScore(score);
                                    scoresListDouble.add(doubleScore);
                                } catch (Exception e) {
                                    throw new JasperReportException("Level bar graph score column contains unknown value: " + score, e);
                                }
                            }
                            scoreAchieved += applyScoreFunction(scoresListDouble, sourceLevelBarGraph.getScoreFunction());
                        }

                        //Get score achieved for this attribute
                        scoreAchieved = scoreAchieved / criterionScoreMap.size();


                        //Set score achieved for this attribute
                        if (scoreAchieved > 0.85) {
                            if (i == 1) {
                                levelCheckValue += 2;
                            } else {
                                levelCheckValue += 1;
                            }
                        } else if (scoreAchieved > 0.5) {
                            if (i == 1) {
                                levelCheckValue += 1;
                            } else {
                                levelCheckValue += 0.5;
                            }
                        }
                    }

                    //0 - not achieved, 1 - all defined attributes are largely achieved, 2- all are fully
                    if (levelCheckValue == 2) {
                        levelAchieved += 1;
                    } else {
                        //All attributes are at least largely achieved
                        if (levelCheckValue >= 1) {
                            levelAchieved += 1;
                        }
                        //We need to have all attributes fully to continue
                        previousLevelAchieved = false;
                    }
                }

                levelMap.put(process, levelAchieved);
            }
            dataMap.put(source.getSourceName(), levelMap);
        }

        //Fill all missing processes with 0
        ArrayList<String> allProcessList = new ArrayList<>(allProcessSet);
        Collections.sort(allProcessList, new NaturalOrderComparator());
        for (String process : allProcessList) {
            for (var dataKey : dataMap.keySet()) {
                if (!dataMap.get(dataKey).containsKey(process)) {
                    dataMap.get(dataKey).put(process, 0);
                }
            }
        }

        //Combine sources with defined merge function
        //Result data in format {combined data: {process1: level, process2: level}}
        LinkedHashMap<String, Map<String, Integer>> resultDataMap = new LinkedHashMap<>();
        if (sourceLevelBarGraph.getMergeScores() != null) {
            resultDataMap.put("Combined data for sources", new LinkedHashMap<>());
            for (String process : allProcessList) {
                boolean firstIteration = true;
                for (var dataKey : dataMap.keySet()) {
                    //Put initial process level
                    if (firstIteration) {
                        resultDataMap.get("Combined data for sources").put(process, dataMap.get(dataKey).get(process));
                        firstIteration = false;
                        continue;
                    }

                    Integer sourceProcessLevel = dataMap.get(dataKey).get(process);
                    switch (sourceLevelBarGraph.getMergeScores()) {
                        case MIN:
                            if (sourceProcessLevel < resultDataMap.get("Combined data for sources").get(process)) {
                                resultDataMap.get("Combined data for sources").put(process, sourceProcessLevel);
                            }
                            ;
                            break;
                        //MAX
                        default:
                            if (sourceProcessLevel > resultDataMap.get("Combined data for sources").get(process)) {
                                resultDataMap.get("Combined data for sources").put(process, sourceProcessLevel);
                            }
                            ;
                            break;
                    }
                }
            }
        } else {
            resultDataMap = dataMap;
        }

        return resultDataMap;
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
