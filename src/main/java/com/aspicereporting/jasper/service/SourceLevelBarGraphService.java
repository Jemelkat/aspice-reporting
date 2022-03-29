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
        //Stores all existing processes across sources
        Set<String> allProcessSet = new HashSet<>();

        //Use process filters as process names - if they are not defined array will stay empty
        List<String> processNames = new ArrayList<>(sourceLevelBarGraph.getProcessFilter());
        Collections.sort(processNames, new NaturalOrderComparator());
        allProcessSet.addAll(processNames);

        //Precompile regex pattern for assessor regex filter
        Pattern assessorPattern = Pattern.compile(StringUtils.isEmpty(sourceLevelBarGraph.getAssessorFilter()) ? "" : sourceLevelBarGraph.getAssessorFilter());
        Matcher assessorMatcher = assessorPattern.matcher("");

        //Result data in format {sourceName: {process1: level, process2: level}, sourceName2: ...}
        LinkedHashMap<String, Map<String, Integer>> dataMap = new LinkedHashMap<>();
        //Iterate over each source defined and get element levels
        for (Source source : sourceLevelBarGraph.getSources()) {
            SourceColumn assessorColumn = getSourceColumnByName(source, sourceLevelBarGraph.getAssessorColumn());
            SourceColumn processColumn = getSourceColumnByName(source, sourceLevelBarGraph.getProcessColumn());
            SourceColumn criterionColumn = getSourceColumnByName(source, sourceLevelBarGraph.getCriterionColumn());
            SourceColumn attributeColumn = getSourceColumnByName(source, sourceLevelBarGraph.getAttributeColumn());
            SourceColumn scoreColumn = getSourceColumnByName(source, sourceLevelBarGraph.getScoreColumn());

            //Get all process names if process filter is not defined
            if (sourceLevelBarGraph.getProcessFilter().isEmpty()) {
                List<String> currentProcessNames = sourceRepository.findDistinctColumnValuesForColumn(processColumn.getId());
                allProcessSet.addAll(currentProcessNames);
                processNames.addAll(currentProcessNames);
                Collections.sort(processNames, new NaturalOrderComparator());
            }

            // Get all data to MAP for faster lookup
            // {(process,attribute) : [{criterion: {assessor: score}, {criterion: {assessor: score}], ...}
            MultiKeyMap valuesMap = new MultiKeyMap();
            for (int i = 0; i < scoreColumn.getSourceData().size(); i++) {
                String process = processColumn.getSourceData().get(i).getValue();
                String attribute = attributeColumn.getSourceData().get(i).getValue();
                String criterion = criterionColumn.getSourceData().get(i).getValue();
                String score = scoreColumn.getSourceData().get(i).getValue();
                String assessor = assessorColumn.getSourceData().get(i).getValue();

                //Filter by process
                if (!sourceLevelBarGraph.getProcessFilter().isEmpty()) {
                    if (!processNames.contains(process)) {
                        continue;
                    }
                }
                //Filter by assessor
                if (!StringUtils.isEmpty(sourceLevelBarGraph.getAssessorFilter())) {
                    assessorMatcher.reset(assessor);
                    if (!assessorMatcher.matches()) {
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

            //Contains process: level for each process in source
            LinkedHashMap<String, Integer> levelMap = new LinkedHashMap<>();
            for (String process : processNames) {
                resetVariables();
                for (int i = 1; i <= 5; i++) {
                    if (!previousLevelAchieved) {
                        break;
                    }
                    resetCheckVariable();
                    for (String attribute : processAttributesMap.get(i)) {
                        double scoreAchieved = 0;
                        MultiKey multikey = new MultiKey(process, attribute);
                        //Process does not have this attribute defined we don't have to increase level
                        if (!valuesMap.containsKey(multikey)) {
                            break;
                        }

                        //Get all criterion scores for (process, attribute, assessor) key and apply score function on them
                        Map<String, Map<String, String>> criterionAssessorMap = (Map<String, Map<String, String>>) valuesMap.get(multikey);
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
                        scoreAchieved = scoresList.stream().mapToDouble(a -> a).sum() / scoresList.size();
                        calculateLevelCheckValue(scoreAchieved, i);
                    }
                    evaulateLevelCheckToLevel();
                }
                //Add all this process level achieved to map
                levelMap.put(process, levelAchieved);
            }
            //Add all process levels for this source to map
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

        //Merge levels for each process with defined merge function
        //Result data in format {combined data: {process1: level, process2: level}}
        LinkedHashMap<String, Map<String, Integer>> resultDataMap = new LinkedHashMap<>();
        if (sourceLevelBarGraph.getMergeScores() != null) {
            String seriesName = sourceLevelBarGraph.getMergeScores().name() + " levels for sources";
            resultDataMap.put(seriesName, new LinkedHashMap<>());
            for (String process : allProcessList) {
                boolean firstIteration = true;
                for (var dataKey : dataMap.keySet()) {
                    //Put initial process level
                    if (firstIteration) {
                        resultDataMap.get(seriesName).put(process, dataMap.get(dataKey).get(process));
                        firstIteration = false;
                        continue;
                    }

                    Integer sourceProcessLevel = dataMap.get(dataKey).get(process);
                    switch (sourceLevelBarGraph.getMergeScores()) {
                        case MIN:
                            if (sourceProcessLevel < resultDataMap.get(seriesName).get(process)) {
                                resultDataMap.get(seriesName).put(process, sourceProcessLevel);
                            }
                            break;
                        //MAX
                        default:
                            if (sourceProcessLevel > resultDataMap.get(seriesName).get(process)) {
                                resultDataMap.get(seriesName).put(process, sourceProcessLevel);
                            }
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
