package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.enums.Orientation;
import com.aspicereporting.entity.enums.ScoreFunction;
import com.aspicereporting.entity.items.LevelBarGraph;
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
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class LevelBarGraphService extends BaseChartService {
    @Autowired
    SourceRepository sourceRepository;
    /**
     * Creates JRDesignImage (level bar graph) which can be used in JasperDesign
     */
    public JRDesignImage createElement(JasperDesign jasperDesign, LevelBarGraph levelBarGraph, Integer counter, Map<String, Object> parameters) throws JRException {
        LinkedHashMap<String, LinkedHashMap<String, Integer>> graphData = getData(levelBarGraph);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (var process : graphData.keySet()) {
            for (var assessor : graphData.get(process).keySet()) {
                Integer level = graphData.get(process).get(assessor);
                dataset.addValue(level, assessor, process);
            }
        }


        final JFreeChart chart = ChartFactory.createBarChart(levelBarGraph.getTitle(),                                   // chart title
                "Process",                  // domain axis label
                "Level",                     // range axis label
                dataset,                            // data
                levelBarGraph.getOrientation().equals(Orientation.VERTICAL) ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL,           // the plot orientation
                true,                        // legend
                false,                        // tooltips
                false                        // urls
        );
        this.applyBarGraphTheme(chart);
        chart.getTitle().setPaint(Color.BLACK);
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

    /**
     * Gets all result data based on item settings
     */
    public LinkedHashMap<String, LinkedHashMap<String, Integer>> getData(LevelBarGraph levelBarGraph) {
        //Stores all existing processes across sources
        Set<String> allProcessSet = new HashSet<>();

        //Use process filters as process names - if they are not defined array will stay empty
        List<String> processFilter = new ArrayList<>(levelBarGraph.getProcessFilter());
        processFilter = processFilter.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        allProcessSet.addAll(processFilter);
        Collections.sort(processFilter, new NaturalOrderComparator());

        //Contains process: level for each process in source
        LinkedHashMap<String, Map<String, Map<String, Integer>>> levelsAchievedMap = new LinkedHashMap<>();
        for (Source source : levelBarGraph.getSources()) {
            SourceColumn assessorColumn = getSourceColumnByName(source, levelBarGraph.getAssessorColumnName());
            SourceColumn processColumn = getSourceColumnByName(source, levelBarGraph.getProcessColumnName());
            SourceColumn criterionColumn = getSourceColumnByName(source, levelBarGraph.getCriterionColumnName());
            SourceColumn attributeColumn = getSourceColumnByName(source, levelBarGraph.getAttributeColumnName());
            SourceColumn scoreColumn = getSourceColumnByName(source, levelBarGraph.getScoreColumnName());

            List<String> assessorFilter = sourceRepository.findDistinctColumnValuesForColumn(assessorColumn.getId());
            assessorFilter = assessorFilter.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
            if (!levelBarGraph.getAssessorFilter().isEmpty()) {
                assessorFilter = assessorFilter.stream().filter(assessor -> levelBarGraph.getAssessorFilter().contains(assessor)).collect(Collectors.toList());
            }

            //Get all process names if process filter is not defined
            if (levelBarGraph.getProcessFilter().isEmpty()) {
                List<String> currentProcessNames = sourceRepository.findDistinctColumnValuesForColumn(processColumn.getId());
                currentProcessNames = currentProcessNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
                allProcessSet.addAll(currentProcessNames);
                processFilter.addAll(currentProcessNames);
                Collections.sort(processFilter, new NaturalOrderComparator());
            }


            // Get all data to MAP for faster lookup*
            MultiKeyMap valuesMap = prepareDataMap(levelBarGraph, scoreColumn, processColumn, attributeColumn, criterionColumn, assessorColumn, processFilter, assessorFilter);
            LinkedHashMap<String, Map<String, Integer>> assesorLevelMap = new LinkedHashMap<>();
            if (levelBarGraph.getAggregateScoresFunction().equals(ScoreFunction.NONE) || levelBarGraph.isAggregateLevels()) {
                assesorLevelMap = getLevelsByLevel(levelBarGraph, valuesMap, processFilter, assessorFilter);
            } else {
                assesorLevelMap = getLevelsByScore(levelBarGraph, valuesMap, processFilter);
            }

            levelsAchievedMap.put(source.getSourceName(), assesorLevelMap);
        }
        //Result data in format {sourcename1: {process1: level, process2: level}, sourceName2: ...}
        LinkedHashMap<String, LinkedHashMap<String, Integer>> dataMap;

        dataMap = mergeSources(levelsAchievedMap, levelBarGraph.getAggregateSourcesFunction());

        ArrayList<String> allProcessList = new ArrayList<>(allProcessSet);
        Collections.sort(allProcessList, new NaturalOrderComparator());

        if (dataMap.isEmpty()) {
            if (allProcessList.isEmpty()) {
                dataMap.put("", new LinkedHashMap<>(Map.of("No measurements found.", 0)));
            } else {
                for (String process : allProcessList) {
                    dataMap.put(process, new LinkedHashMap<>(Map.of("No measurements found.", 0)));
                }
            }
        }

        return dataMap;
    }

    /**
     * Returns data in map format for easier lookup
     * {(process,attribute): [{criterion1:[{assessor1: score,assessor2: score}]},{criterion2: [{assessor1: score,assessor2: score}]},...}
     */
    private MultiKeyMap prepareDataMap(LevelBarGraph levelBarGraph, SourceColumn scoreColumn, SourceColumn processColumn, SourceColumn attributeColumn, SourceColumn criterionColumn, SourceColumn assessorColumn, List<String> processFilter, List<String> assessorFilter) {
        MultiKeyMap valuesMap = new MultiKeyMap();
        for (int i = 0; i < scoreColumn.getSourceData().size(); i++) {
            String process = processColumn.getSourceData().get(i).getValue();
            String attribute = attributeColumn.getSourceData().get(i).getValue();
            String criterion = criterionColumn.getSourceData().get(i).getValue();
            String score = scoreColumn.getSourceData().get(i).getValue();
            String assessor = assessorColumn.getSourceData().get(i).getValue();

            //Filter by process
            if (!levelBarGraph.getProcessFilter().isEmpty()) {
                if (!processFilter.contains(process)) {
                    continue;
                }
            }
            if (!levelBarGraph.getAssessorFilter().isEmpty()) {
                if (!assessorFilter.contains(assessor)) {
                    continue;
                }
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

    /**
     * Method creates process level map, where each process has level achieved for each assessor (assessor scores are not merged in any way)
     * This method is capable of merging the result levels to get MIN/MAX level process levels
     *
     * @return {process: assessor: level} or {process: MIN/MAX levels achieved: level}
     */
    private LinkedHashMap<String, Map<String, Integer>> getLevelsByLevel(LevelBarGraph levelBarGraph, MultiKeyMap valuesMap, List<String> processFilter, List<String> assessorFilter) {
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
                            throw new JasperReportException("Level bar graph id = " + levelBarGraph.getId() + " score column contains unknown value: ", e);
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

        if (levelBarGraph.isAggregateLevels()) {
            processLevelMap = mergeAssessors(processLevelMap, levelBarGraph.getAggregateScoresFunction());
        }
        return processLevelMap;
    }

    /**
     * Method creates process level map, where each process level is calaculated by aggregating of assessor scores by defined function
     *
     * @return {process: MIN/MAX/AVG level by score: level}
     */
    private LinkedHashMap<String, Map<String, Integer>> getLevelsByScore(LevelBarGraph levelBarGraph, MultiKeyMap valuesMap, List<String> processFilter) {
        LinkedHashMap<String, Map<String, Integer>> processLevelMap = new LinkedHashMap<>();
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
                            scoresList.add(applyScoreFunction(convertScoresToDoubles(assessorScoreList), levelBarGraph.getAggregateScoresFunction()));
                        } catch (JasperReportException e) {
                            throw new JasperReportException("Level bar graph id = " + levelBarGraph.getId() + " graph score column contains unknown value: ", e);
                        }
                    }
                    //Get attribute score achieved as (sum of scores/count of scores)
                    calculateLevelCheckValue(scoresList.stream().mapToDouble(a -> a).sum() / scoresList.size(), i);
                }
                evaulateLevelCheckToLevel();
            }

            if (processLevelMap.containsKey(process)) {
                processLevelMap.get(process).put(levelBarGraph.getAggregateScoresFunction().name() + " scores achieved", levelAchieved);
            } else {
                processLevelMap.put(process, new HashMap<>(Map.of(levelBarGraph.getAggregateScoresFunction().name() + " scores achieved", levelAchieved)));
            }
        }
        return processLevelMap;
    }

    /**
     * For each process merges all assessor levels achieved by aggregate function
     */
    private LinkedHashMap<String, Map<String, Integer>> mergeAssessors(LinkedHashMap<String, Map<String, Integer>> processLevelMap, ScoreFunction scoreFunction) {
        LinkedHashMap<String, Map<String, Integer>> updatedMap = new LinkedHashMap<>();
        for (String process : processLevelMap.keySet()) {
            List<Integer> levels = List.copyOf(processLevelMap.get(process).values());
            updatedMap.put(process, new HashMap<>(Map.of(scoreFunction.name() + " levels achieved", applyMinMaxFunction(levels, scoreFunction))));
        }
        return updatedMap;
    }

    private LinkedHashMap<String, LinkedHashMap<String, Integer>> mergeSources(LinkedHashMap<String, Map<String, Map<String, Integer>>> levelsAchievedMap, ScoreFunction scoreFunction) {
        LinkedHashMap<String, LinkedHashMap<String, Integer>> updatedMap = new LinkedHashMap<>();
        //Only one source defined
        if (levelsAchievedMap.keySet().size() == 1 || scoreFunction.equals(ScoreFunction.NONE)) {
            for (String source : levelsAchievedMap.keySet()) {
                for (String process : levelsAchievedMap.get(source).keySet()) {
                    for (String assessor : levelsAchievedMap.get(source).get(process).keySet()) {
                        String sourceAssessor = assessor;
                        if (scoreFunction.equals(ScoreFunction.NONE) && levelsAchievedMap.keySet().size() > 1) {
                            sourceAssessor = assessor + "-" + source;
                        }
                        if (updatedMap.containsKey(process)) {
                            updatedMap.get(process).put(sourceAssessor, levelsAchievedMap.get(source).get(process).get(assessor));
                        } else {
                            updatedMap.put(process, new LinkedHashMap<>(Map.of(sourceAssessor, levelsAchievedMap.get(source).get(process).get(assessor))));
                        }
                    }
                }
            }
        } else {

            LinkedHashMap<String, Map<String, ArrayList<Integer>>> combinedLevels = new LinkedHashMap<>();
            for (String source : levelsAchievedMap.keySet()) {
                for (String process : levelsAchievedMap.get(source).keySet()) {
                    for (String assessor : levelsAchievedMap.get(source).get(process).keySet()) {
                        if (combinedLevels.containsKey(process)) {
                            Map<String, ArrayList<Integer>> assesorLevels = combinedLevels.get(process);
                            if (assesorLevels.containsKey(assessor)) {
                                assesorLevels.get(assessor).add(levelsAchievedMap.get(source).get(process).get(assessor));
                            } else {
                                assesorLevels.put(assessor, new ArrayList<>(List.of(levelsAchievedMap.get(source).get(process).get(assessor))));
                            }
                        } else {
                            combinedLevels.put(process, new HashMap<>(Map.of(assessor, new ArrayList<>(List.of(levelsAchievedMap.get(source).get(process).get(assessor))))));
                        }
                    }
                }
            }
            for (String process : combinedLevels.keySet()) {
                for (String assessor : combinedLevels.get(process).keySet()) {
                    Integer level = applyMinMaxFunction(combinedLevels.get(process).get(assessor), scoreFunction);
                    if (updatedMap.containsKey(process)) {
                        if (scoreFunction.equals(ScoreFunction.MIN) && level < updatedMap.get(process).get(scoreFunction.name() + " levels achieved in sources")) {
                            updatedMap.get(process).put(scoreFunction.name() + " levels achieved in sources", level);
                        } else {
                            if (level > updatedMap.get(process).get(scoreFunction.name() + " levels achieved in sources")) {
                                updatedMap.get(process).put(scoreFunction.name() + " levels achieved in sources", level);
                            }
                        }
                    } else {
                        updatedMap.put(process, new LinkedHashMap<>(Map.of(scoreFunction.name() + " levels achieved in sources", applyMinMaxFunction(combinedLevels.get(process).get(assessor), scoreFunction))));
                    }
                }
            }
        }
        return updatedMap;
    }

    private SourceColumn getSourceColumnByName(Source source, String name) {
        for (SourceColumn sourceColumn : source.getSourceColumns()) {
            if (sourceColumn.getColumnName().equals(name)) {
                return sourceColumn;
            }
        }
        throw new InvalidDataException("Level bar graph source: " + source.getSourceName() + " has no column named: " + name);
    }
}
