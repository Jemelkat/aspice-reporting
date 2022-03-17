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

@Service
public class SourceLevelBarGraphService extends BaseChartService {
    @Autowired
    SourceRepository sourceRepository;

    public JRDesignImage createElement(JasperDesign jasperDesign, SourceLevelBarGraph sourceLevelBarGraph, Integer counter, Map<String, Object> parameters) throws JRException {
        LinkedHashMap<String, Map<String, Integer>> graphData = getData(sourceLevelBarGraph);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int maxLevel = 0;
        for (var process : graphData.keySet()) {
            for(var assessor : graphData.get(process).keySet()) {
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
        rangeAxis.setUpperBound(maxLevel < 5 ? maxLevel + 1 : maxLevel);
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

        //Precompile regex pattern or use first assessor found as assessor
        Pattern assessorPattern = Pattern.compile(sourceLevelBarGraph.getAssessorFilter()!=null ? sourceLevelBarGraph.getAssessorFilter().trim():"");
        Matcher assessorMatcher = assessorPattern.matcher("");
        String firstAssessor = null;

        for (Source source : sourceLevelBarGraph.getSources()) {
            SourceColumn assessorColumn = getSourceColumnByName(source, sourceLevelBarGraph.getAssessorColumn());
            SourceColumn processColumn = getSourceColumnByName(source, sourceLevelBarGraph.getProcessColumn());
            SourceColumn attributeColumn = getSourceColumnByName(source, sourceLevelBarGraph.getAttributeColumn());
            SourceColumn scoreColumn = getSourceColumnByName(source, sourceLevelBarGraph.getScoreColumn());

            //Get all process names sorted - only if not defined in filter
            if(sourceLevelBarGraph.getProcessFilter().isEmpty()) {
                List<String> currentProcessNames = sourceRepository.findDistinctColumnValuesForColumn(processColumn.getId());
                allProcessSet.addAll(currentProcessNames);
                processNames.addAll(currentProcessNames);
                Collections.sort(processNames, new NaturalOrderComparator());
            }

            //Get first assessor if assessor filter is not defined
            if(firstAssessor==null && (sourceLevelBarGraph.getAssessorFilter() == null || sourceLevelBarGraph.getAssessorFilter().trim().isEmpty())) {
                List<String> assessorNames = sourceRepository.findDistinctColumnValuesForColumn(assessorColumn.getId());
                if(!assessorNames.isEmpty()) {
                    firstAssessor = assessorNames.get(0).trim();
                } else {
                    throw new InvalidDataException("Source: " +source.getSourceName() + " has no assessors defined.");
                }
            }

            //Get all related data to map for easier lookup
            MultiKeyMap sourceDataMap = new MultiKeyMap();
            for (int i = 0; i < scoreColumn.getSourceData().size(); i++) {
                String process = processColumn.getSourceData().get(i).getValue().trim();
                String attribute = attributeColumn.getSourceData().get(i).getValue().trim();
                String score = scoreColumn.getSourceData().get(i).getValue().trim();
                String assessor = assessorColumn.getSourceData().get(i).getValue().trim();

                //Filter by assessor filter or first assessor found in first source file
                if(firstAssessor == null) {
                    assessorMatcher.reset(assessor);
                    if(!assessorMatcher.matches()) {
                        continue;
                    }
                } else {
                    if(!firstAssessor.equals(assessor)) {
                        continue;
                    }
                }

                //TODO add performance criterion to detect duplicate data
                MultiKey key = new MultiKey(process, attribute);
                if (sourceDataMap.containsKey(key)) {
                    ((ArrayList) sourceDataMap.get(key)).add(score);
                } else {
                    sourceDataMap.put(key, new ArrayList<>(Arrays.asList(score)));
                }
            }

            LinkedHashMap<String, Integer> levelMap = new LinkedHashMap<>();
            for (String processName : processNames) {

                int levelAchieved = 0;
                boolean previousLevelAchieved = true;
                for (int i = 1; i <= 5; i++) {
                    double levelValue = 0;
                    if (!previousLevelAchieved) {
                        break;
                    }
                    for (String attributeName : processAttributesMap.get(i)) {
                        double scoreAchieved = 0;
                        MultiKey key = new MultiKey(processName, attributeName);
                        if(sourceDataMap.containsKey(key)) {
                            for (String s : ((ArrayList<String>)sourceDataMap.get(key))) {
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
                                scoreAchieved += score;
                            }
                            //Get average score achieved for this attribute
                            scoreAchieved = scoreAchieved / ((ArrayList<String>)sourceDataMap.get(key)).size();
                        }

                        //Set score achieved for this attribute
                        if (scoreAchieved > 0.85) {
                            if (i == 1) {
                                levelValue += 2;
                            } else {
                                levelValue += 1;
                            }
                        } else if (scoreAchieved > 0.5) {
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

                levelMap.put(processName, levelAchieved);
            }
            dataMap.put(source.getSourceName(), levelMap);
        }

        //TODO fill all missing processes for each source - keep set of all processes then check against it
        ArrayList<String> allProcessList = new ArrayList<>(allProcessSet);
        Collections.sort(allProcessList, new NaturalOrderComparator());
        for(String process : allProcessList) {
            for(var dataKey : dataMap.keySet()) {
                if(!dataMap.get(dataKey).containsKey(process)) {
                    dataMap.get(dataKey).put(process, 0);
                }
            }
        }
        return dataMap;
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
