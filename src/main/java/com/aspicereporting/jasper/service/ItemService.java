package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.enums.ScoreFunction;
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
    protected static Map<Integer, ArrayList<String>> processAttributesMap = Map.ofEntries(
            new AbstractMap.SimpleEntry<>(1, new ArrayList<>(Arrays.asList("PA1.1"))),
            new AbstractMap.SimpleEntry<>(2, new ArrayList<>(Arrays.asList("PA2.1", "PA2.2"))),
            new AbstractMap.SimpleEntry<>(3, new ArrayList<>(Arrays.asList("PA3.1", "PA3.2"))),
            new AbstractMap.SimpleEntry<>(4, new ArrayList<>(Arrays.asList("PA4.1", "PA4.2"))),
            new AbstractMap.SimpleEntry<>(5, new ArrayList<>(Arrays.asList("PA5.1", "PA5.2")))
    );

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

    public Double applyScoreFunction(List<Double> scoresList, ScoreFunction scoreFunction) {
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
