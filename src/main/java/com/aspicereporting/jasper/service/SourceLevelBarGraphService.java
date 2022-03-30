package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.enums.Orientation;
import com.aspicereporting.entity.enums.ScoreFunction;
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
import org.apache.commons.lang3.StringUtils;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SourceLevelBarGraphService extends BaseChartService {
    @Autowired
    SourceRepository sourceRepository;

    public JRDesignImage createElement(JasperDesign jasperDesign, SourceLevelBarGraph sourceLevelBarGraph, Integer counter, Map<String, Object> parameters) throws JRException {
        LinkedHashMap<String, Map<String, Integer>> graphData = getData(sourceLevelBarGraph);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (var process : graphData.keySet()) {
            for (var assessor : graphData.get(process).keySet()) {
                Integer level = graphData.get(process).get(assessor);
                dataset.addValue(level, assessor, process);
            }
        }


        final JFreeChart chart = ChartFactory.createBarChart("",                                   // chart title
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
        //Stores all existing processes across sources
        Set<String> allProcessSet = new HashSet<>();
        Set<String> allAssessorsSet = new HashSet<>();

        //Use process filters as process names - if they are not defined array will stay empty
        List<String> processFilter = new ArrayList<>(sourceLevelBarGraph.getProcessFilter());
        processFilter = processFilter.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
        Collections.sort(processFilter, new NaturalOrderComparator());

        List<String> assessorFilter = new ArrayList<>(List.of("Assesor1", "Assesor3"));

        //Contains process: level for each process in source
        LinkedHashMap<String, Map<String, Map<String, Integer>>> levelsAchievedMap = new LinkedHashMap<>();
        for (Source source : sourceLevelBarGraph.getSources()) {
            SourceColumn assessorColumn = getSourceColumnByName(source, sourceLevelBarGraph.getAssessorColumn());
            SourceColumn processColumn = getSourceColumnByName(source, sourceLevelBarGraph.getProcessColumn());
            SourceColumn criterionColumn = getSourceColumnByName(source, sourceLevelBarGraph.getCriterionColumn());
            SourceColumn attributeColumn = getSourceColumnByName(source, sourceLevelBarGraph.getAttributeColumn());
            SourceColumn scoreColumn = getSourceColumnByName(source, sourceLevelBarGraph.getScoreColumn());

            //Get all process names if process filter is not defined
            if (sourceLevelBarGraph.getProcessFilter().isEmpty()) {
                List<String> currentProcessNames = sourceRepository.findDistinctColumnValuesForColumn(processColumn.getId());
                currentProcessNames = currentProcessNames.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
                allProcessSet.addAll(currentProcessNames);
                processFilter.addAll(currentProcessNames);
                Collections.sort(processFilter, new NaturalOrderComparator());
            }

            //Stores all assessors in this source file
            Set<String> assessorsSet = new HashSet<>();

            // Get all data to MAP for faster lookup*
            MultiKeyMap valuesMap = prepareDataMap(sourceLevelBarGraph, scoreColumn, processColumn, attributeColumn, criterionColumn, assessorColumn, processFilter, assessorsSet);
            LinkedHashMap<String, Map<String, Integer>> assesorLevelMap = new LinkedHashMap<>();
            if (sourceLevelBarGraph.getScoreFunction().equals(ScoreFunction.NONE) || sourceLevelBarGraph.isMergeLevels()) {
                assesorLevelMap = getLevelsByLevel(sourceLevelBarGraph, valuesMap, processFilter, assessorFilter);
            } else {
                assesorLevelMap = getLevelsByScore(sourceLevelBarGraph, valuesMap, processFilter);
            }

            levelsAchievedMap.put(source.getSourceName(), assesorLevelMap);
        }
        //Result data in format {sourcename1: {process1: level, process2: level}, sourceName2: ...}
        LinkedHashMap<String, Map<String, Integer>> dataMap = new LinkedHashMap<>();

        dataMap = mergeSources(levelsAchievedMap, sourceLevelBarGraph.getMergeScores());

        ArrayList<String> allProcessList = new ArrayList<>(allProcessSet);
        Collections.sort(allProcessList, new NaturalOrderComparator());
//        for (String process : allProcessList) {
//            //Get levels for process across all sources
//            List<String> sourceNames = new ArrayList<>();
//            List<Integer> processLevels = new ArrayList<>();
//            for (String source : levelsAchievedMap.keySet()) {
//                sourceNames.add(source);
//                if (levelsAchievedMap.get(source).get("test").containsKey(process)) {
//                    processLevels.add(levelsAchievedMap.get(source).get("test").get(process));
//                } else {
//                    processLevels.add(0);
//                }
//
//            }
//
//            //Merge process levels or create record for each source
//            switch (sourceLevelBarGraph.getMergeScores()) {
//                case MAX:
//                    if (!dataMap.containsKey("MAX levels")) {
//                        dataMap.put("MAX levels", new LinkedHashMap<>());
//                    }
//                    dataMap.get("MAX levels").put(process, applyMinMaxFunction(processLevels, sourceLevelBarGraph.getMergeScores()));
//                    break;
//                case MIN:
//                    if (!dataMap.containsKey("MIN levels")) {
//                        dataMap.put("MIN levels", new LinkedHashMap<>());
//                    }
//                    dataMap.get("MIN levels").put(process, applyMinMaxFunction(processLevels, sourceLevelBarGraph.getMergeScores()));
//                    break;
//                default:
//                    for (int i = 0; i < sourceNames.size(); i++) {
//                        String source = sourceNames.get(i);
//                        Integer level = processLevels.get(i);
//                        if (!dataMap.containsKey(source)) {
//                            dataMap.put(source, new LinkedHashMap<>());
//                        }
//                        dataMap.get(source).put(process, level);
//                    }
//                    break;
//            }
//        }

        //Fill all missing processes with level 0
//        for (String process : allProcessList) {
//            for (var dataKey : dataMap.keySet()) {
//                if (!dataMap.get(dataKey).containsKey(process)) {
//                    dataMap.get(dataKey).put(process, 0);
//                }
//            }
//        }

        if(dataMap.isEmpty()) {
            dataMap.put("", new HashMap<>(Map.of("No data found for any process.", 0)));
        }

        return dataMap;
    }

    /**
     * Returns data in map format for easier lookup
     * {(process,attribute): [{criterion1:[{assessor1: score,assessor2: score}]},{criterion2: [{assessor1: score,assessor2: score}]},...}
     */
    private MultiKeyMap prepareDataMap(SourceLevelBarGraph sourceLevelBarGraph, SourceColumn scoreColumn, SourceColumn processColumn, SourceColumn attributeColumn, SourceColumn criterionColumn, SourceColumn assessorColumn, List<String> processFilter, Set<String> assessorsSet) {
        //Precompile regex pattern for assessor regex filter
        Pattern assessorPattern = Pattern.compile(StringUtils.isEmpty(sourceLevelBarGraph.getAssessorFilter()) ? "" : sourceLevelBarGraph.getAssessorFilter());
        Matcher assessorMatcher = assessorPattern.matcher("");

        MultiKeyMap valuesMap = new MultiKeyMap();
        for (int i = 0; i < scoreColumn.getSourceData().size(); i++) {
            String process = processColumn.getSourceData().get(i).getValue();
            String attribute = attributeColumn.getSourceData().get(i).getValue();
            String criterion = criterionColumn.getSourceData().get(i).getValue();
            String score = scoreColumn.getSourceData().get(i).getValue();
            String assessor = assessorColumn.getSourceData().get(i).getValue();

            //Filter by process
            if (!sourceLevelBarGraph.getProcessFilter().isEmpty()) {
                if (!processFilter.contains(process)) {
                    continue;
                }
            }
            //Filter by assessor
            if (!StringUtils.isEmpty(sourceLevelBarGraph.getAssessorFilter())) {
                assessorMatcher.reset(assessor);
                if (!assessorMatcher.matches()) {
                    assessorsSet.add(assessor);
                    continue;
                }
            } else {
                assessorsSet.add(assessor);
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
    private LinkedHashMap<String, Map<String, Integer>> getLevelsByLevel(SourceLevelBarGraph sourceLevelBarGraph, MultiKeyMap valuesMap, List<String> processFilter, List<String> assessorFilter) {
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

        if (sourceLevelBarGraph.isMergeLevels()) {
            processLevelMap = mergeAssessors(processLevelMap, sourceLevelBarGraph.getScoreFunction());
        }
        return processLevelMap;
    }

    /**
     * Method creates process level map, where each process level is calaculated by aggregating of assessor scores by defined function
     *
     * @return {process: MIN/MAX/AVG level by score: level}
     */
    private LinkedHashMap<String, Map<String, Integer>> getLevelsByScore(SourceLevelBarGraph sourceLevelBarGraph, MultiKeyMap valuesMap, List<String> processFilter) {
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
                            scoresList.add(applyScoreFunction(convertScoresToDoubles(assessorScoreList), sourceLevelBarGraph.getScoreFunction()));
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
                processLevelMap.get(process).put(sourceLevelBarGraph.getScoreFunction().name() + " scores achieved", levelAchieved);
            } else {
                processLevelMap.put(process, new HashMap<>(Map.of(sourceLevelBarGraph.getScoreFunction().name() + " scores achieved", levelAchieved)));
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

    private LinkedHashMap<String, Map<String, Integer>> mergeSources(LinkedHashMap<String, Map<String, Map<String, Integer>>> levelsAchievedMap, ScoreFunction scoreFunction) {
        LinkedHashMap<String, Map<String, Integer>> updatedMap = new LinkedHashMap<>();
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
                            updatedMap.put(process, new HashMap<>(Map.of(sourceAssessor, levelsAchievedMap.get(source).get(process).get(assessor))));
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
                    if (updatedMap.containsKey(process)) {
                        updatedMap.get(process).put(scoreFunction.name() + " levels achieved in sources", applyMinMaxFunction(combinedLevels.get(process).get(assessor), scoreFunction));
                    } else {
                        updatedMap.put(process, new HashMap<>(Map.of(scoreFunction.name() + " levels achieved in sources", applyMinMaxFunction(combinedLevels.get(process).get(assessor), scoreFunction))));
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
        throw new InvalidDataException("Source level bar graph source: " + source.getSourceName() + " has no column named: " + name);
    }
}
