package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.enums.ScoreFunction;
import com.aspicereporting.exception.JasperReportException;

import java.util.*;

public abstract class AbstractItemService {
    protected static Map<String, Double> scoreToValueMap = Map.ofEntries(
            new AbstractMap.SimpleEntry<>("N", 0D),
            new AbstractMap.SimpleEntry<>("P", 0.33D),
            new AbstractMap.SimpleEntry<>("L", 0.66D),
            new AbstractMap.SimpleEntry<>("F", 1D)
    );
    protected static LinkedHashMap<Double, String> valueToScoreMap = new LinkedHashMap<>() {{
        put(0D, "N");
        put(0.15D, "P");
        put(0.50D, "L");
        put(0.85D, "F");
    }};
    protected static Map<Integer, ArrayList<String>> processAttributesMap = Map.ofEntries(
            new AbstractMap.SimpleEntry<>(1, new ArrayList<>(Arrays.asList("PA1.1"))),
            new AbstractMap.SimpleEntry<>(2, new ArrayList<>(Arrays.asList("PA2.1", "PA2.2"))),
            new AbstractMap.SimpleEntry<>(3, new ArrayList<>(Arrays.asList("PA3.1", "PA3.2"))),
            new AbstractMap.SimpleEntry<>(4, new ArrayList<>(Arrays.asList("PA4.1", "PA4.2"))),
            new AbstractMap.SimpleEntry<>(5, new ArrayList<>(Arrays.asList("PA5.1", "PA5.2")))
    );

    protected Double levelCheckValue = 0D;
    protected Boolean previousLevelAchieved = true;
    protected Integer levelAchieved = 0;

    protected double getValueForScore(String score) {
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

    protected String getScoreForValue(Double value) {
        Double key = 0D;
        for (Double valueKey : valueToScoreMap.keySet()) {
            if (value > valueKey) {
                key = valueKey;
            } else {
                break;
            }
        }
        return valueToScoreMap.get(key);
    }

    protected Double applyScoreFunction(List<Double> scoresList, ScoreFunction scoreFunction) {
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

    protected Integer applyMinMaxFunction(List<Integer> levels, ScoreFunction scoreFunction) {
        if (levels.isEmpty()) {
            return null;
        }

        switch (scoreFunction) {
            case MIN:
                return Collections.min(levels);
            default:
                return Collections.max(levels);
        }
    }

    protected List<Double> convertScoresToDoubles(List<String> scoresList) {
        List<Double> scoresListDouble = new ArrayList<>();
        for (int j = 0; j < scoresList.size(); j++) {
            String score = scoresList.get(j);
            try {
                Double doubleScore = getValueForScore(score);
                scoresListDouble.add(doubleScore);
            } catch (Exception e) {
                throw new JasperReportException(e);
            }
        }
        return scoresListDouble;
    }

    protected void calculateLevelCheckValue(Double scoreAchieved, Integer evaulatingLevel) {
        if (scoreAchieved > 0.85) {
            if (evaulatingLevel == 1) {
                levelCheckValue += 2;
            } else {
                levelCheckValue += 1;
            }
        } else if (scoreAchieved > 0.5) {
            if (evaulatingLevel == 1) {
                levelCheckValue += 1;
            } else {
                levelCheckValue += 0.5;
            }
        }
    }

    //0 - not achieved, 1 - all defined attributes are largely achieved, 2- all are fully
    protected void evaulateLevelCheckToLevel() {
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

    protected void resetVariables() {
        this.levelAchieved = 0;
        this.previousLevelAchieved = true;
        this.levelCheckValue = 0D;
    }

    protected void resetCheckVariable() {
        this.levelCheckValue = 0D;
    }
}
