package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.exception.JasperReportException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public abstract class ItemService {
    protected static Map<String, Double> scoreToValueMap = Map.ofEntries(
            new AbstractMap.SimpleEntry<>("N", 0D),
            new AbstractMap.SimpleEntry<>("P", 0.33D),
            new AbstractMap.SimpleEntry<>("L", 0.66D),
            new AbstractMap.SimpleEntry<>("F", 1D)
    );
    protected static LinkedHashMap<Double, String> valueToScoreMap = new LinkedHashMap<>() {{
        put(0D, "N");
        put(0.33D, "P");
        put(0.66D, "L");
        put(1D, "F");
    }};


    public double getValueForScore(String score) {
        if (scoreToValueMap.containsKey(score)) {
            return scoreToValueMap.get(score);
        } else {
            try {
                return Double.parseDouble(score);
            } catch (Exception e) {
                throw e;
            }
        }
    }

    public String getScoreForValue(Double value) {
        Double key = 0D;
        for (Double valueKey : valueToScoreMap.keySet()) {
            if (value >= valueKey) {
                key = valueKey;
            } else {
                break;
            }
        }
        return valueToScoreMap.get(key);
    }

    public Double applyScoreFunction(List<Double> scoresList, ReportItem.EFunction scoreFunction) {
        if (scoresList.isEmpty()) {
            return null;
        }

        switch (scoreFunction) {
            case MIN:
                return Collections.min(scoresList);
            case MAX:
                return Collections.max(scoresList);
            //AVG
            default:
                return scoresList.stream().mapToDouble(s -> s).average().getAsDouble();
        }
    }
}
