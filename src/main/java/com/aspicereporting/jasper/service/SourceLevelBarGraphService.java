package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.items.CapabilityBarGraph;
import com.aspicereporting.entity.items.SourceLevelBarGraph;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.exception.JasperReportException;
import com.aspicereporting.repository.SourceRepository;
import com.aspicereporting.utils.NaturalOrderComparator;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SourceLevelBarGraphService extends BaseChartService {
    @Autowired
    SourceRepository sourceRepository;

    public LinkedHashMap<String, Map<String, Integer>> getData(SourceLevelBarGraph sourceLevelBarGraph) {
        //Result data in format {sourceName: {process1: level, process2: level}, sourceName2: ...}
        LinkedHashMap<String, Map<String, Integer>> dataMap = new LinkedHashMap<>();

        for (Source source : sourceLevelBarGraph.getSources()) {
            SourceColumn processColumn = getSourceColumnByName(source, sourceLevelBarGraph.getProcessColumn());
            SourceColumn attributeColumn = getSourceColumnByName(source, sourceLevelBarGraph.getAttributeColumn());
            SourceColumn scoreColumn = getSourceColumnByName(source, sourceLevelBarGraph.getScoreColumn());

            //Get all process names sorted
            List<String> processNames = sourceRepository.findDistinctColumnValuesForColumn(processColumn.getId());
            Collections.sort(processNames, new NaturalOrderComparator());

            //Get all related data to map for easier lookup
            MultiKeyMap sourceDataMap = new MultiKeyMap();
            for (int i = 0; i < scoreColumn.getSourceData().size(); i++) {
                String process = processColumn.getSourceData().get(i).getValue();
                String attribute = attributeColumn.getSourceData().get(i).getValue();
                String score = scoreColumn.getSourceData().get(i).getValue();

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

                int level = 0;
                boolean previousLevelAchieved = true;
                for (int i = 1; i <= 5; i++) {
                    double levelValue = 0;
                    if (!previousLevelAchieved) {
                        break;
                    }
                    for (String attributeName : processAttributesMap.get(i)) {
                        double avgScore = 0;
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
                                avgScore += score;
                            }
                            //Get average score achieved for this attribute
                            avgScore = avgScore / ((ArrayList<String>)sourceDataMap.get(key)).size();
                        }

                        //Set score achieved for this attribute
                        if (avgScore > 0.85) {
                            if (i == 1) {
                                levelValue += 2;
                            } else {
                                levelValue += 1;
                            }
                        } else if (avgScore > 0.5) {
                            if (i == 1) {
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
                        previousLevelAchieved = false;
                    }
                }

                levelMap.put(processName, level);
            }
            dataMap.put(source.getSourceName(), levelMap);
        }

        //TODO fill all missing processes for each source - keep set of all processes then check against it
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
