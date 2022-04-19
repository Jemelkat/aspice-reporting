package com.aspicereporting.jasper;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.SourceData;
import com.aspicereporting.entity.enums.ScoreFunction;
import com.aspicereporting.entity.items.LevelBarGraph;
import com.aspicereporting.exception.JasperReportException;
import com.aspicereporting.jasper.service.LevelBarGraphService;
import com.aspicereporting.repository.SourceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class LevelBarGraphTests {

    @Mock
    private SourceRepository sourceRepository;

    @InjectMocks
    private static LevelBarGraphService levelBarGraphService;

    private static Source source;
    private LevelBarGraph levelBarGraph;

    @BeforeAll
    public static void init() {
        levelBarGraphService = new LevelBarGraphService();
        source = new Source();

        SourceColumn assessor = new SourceColumn(0L, "Assessor");
        SourceData sd = new SourceData("Tomas");
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        sd = new SourceData("Jakub");
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);

        SourceColumn process = new SourceColumn(1L, "Process");
        sd = new SourceData("SYS.5");
        process.addSourceData(sd);
        process.addSourceData(sd);
        process.addSourceData(sd);
        sd = new SourceData("SYS.4");
        process.addSourceData(sd);
        process.addSourceData(sd);
        sd = new SourceData("SYS.5");
        process.addSourceData(sd);
        process.addSourceData(sd);
        process.addSourceData(sd);

        SourceColumn attribute = new SourceColumn(2L, "Attribute");
        sd = new SourceData("PA1.1");
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);

        SourceColumn criterion = new SourceColumn(3L, "Criterion");
        criterion.addSourceData(new SourceData("BP1"));
        criterion.addSourceData(new SourceData("BP2"));
        criterion.addSourceData(new SourceData("BP3"));
        criterion.addSourceData(new SourceData("BP1"));
        criterion.addSourceData(new SourceData("BP2"));
        criterion.addSourceData(new SourceData("BP1"));
        criterion.addSourceData(new SourceData("BP2"));
        criterion.addSourceData(new SourceData("BP3"));

        SourceColumn score = new SourceColumn(4L, "Score");
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("F"));
        score.addSourceData(new SourceData("P"));
        score.addSourceData(new SourceData("P"));
        score.addSourceData(new SourceData("P"));
        score.addSourceData(new SourceData("P"));
        score.addSourceData(new SourceData("P"));
        score.addSourceData(new SourceData("P"));

        List<SourceColumn> sourceColumns = Arrays.asList(process, assessor, attribute, criterion, score);
        source.addSourceColumns(sourceColumns);
        source.setSourceName("Source1");
    }

    @BeforeEach
    public void beforeEach() {
        levelBarGraph = new LevelBarGraph();
        levelBarGraph.setSources(Arrays.asList(source));
        levelBarGraph.setAssessorColumnName("Assessor");
        levelBarGraph.setProcessColumnName("Process");
        levelBarGraph.setAttributeColumnName("Attribute");
        levelBarGraph.setCriterionColumnName("Criterion");
        levelBarGraph.setScoreColumnName("Score");
    }

    @Test
    @DisplayName("Empty graph object.")
    public void emptyObjectTest() {
        LevelBarGraph levelBarGraph = new LevelBarGraph();
        LinkedHashMap<String, LinkedHashMap<String, Integer>> dataMap = levelBarGraphService.getData(levelBarGraph);
        Assertions.assertEquals(1, dataMap.keySet().size());
        Assertions.assertEquals(1, dataMap.get("").keySet().size());
        Assertions.assertEquals(1, dataMap.get("").values().size());
        Assertions.assertTrue(dataMap.get("").keySet().contains("No measurements found."));
    }

    @Test
    @DisplayName("Basic graph object.")
    public void basicObjectTest() {
        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"))
                .thenReturn(Arrays.asList("SYS.5", "SYS.4"));

        LinkedHashMap<String, LinkedHashMap<String, Integer>> dataMap = levelBarGraphService.getData(levelBarGraph);
        Assertions.assertEquals(2, dataMap.keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.4").size());
        Assertions.assertEquals(2, dataMap.get("SYS.5").size());
        Assertions.assertEquals(0, dataMap.get("SYS.4").get("Jakub"));
        Assertions.assertEquals(1, dataMap.get("SYS.5").get("Tomas"));
        Assertions.assertEquals(0, dataMap.get("SYS.5").get("Jakub"));
    }

    @Test
    @DisplayName("Assessor filter.")
    public void assessorFilterTest() {
        levelBarGraph.setAssessorFilter(Arrays.asList("Tomas"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"))
                .thenReturn(Arrays.asList("SYS.5", "SYS.4"));

        LinkedHashMap<String, LinkedHashMap<String, Integer>> dataMap = levelBarGraphService.getData(levelBarGraph);
        Assertions.assertEquals(1, dataMap.keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.5").size());
        Assertions.assertEquals(1, dataMap.get("SYS.5").get("Tomas"));
    }

    @Test
    @DisplayName("Process filter.")
    public void processFilterTest() {
        levelBarGraph.setProcessFilter(Arrays.asList("SYS.5"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"))
                .thenReturn(Arrays.asList("SYS.5", "SYS.4"));

        LinkedHashMap<String, LinkedHashMap<String, Integer>> dataMap = levelBarGraphService.getData(levelBarGraph);
        Assertions.assertEquals(1, dataMap.keySet().size());
        Assertions.assertEquals(2, dataMap.get("SYS.5").size());
        Assertions.assertEquals(1, dataMap.get("SYS.5").get("Tomas"));
        Assertions.assertEquals(0, dataMap.get("SYS.5").get("Jakub"));
    }

    @Test
    @DisplayName("Process and assesor filter.")
    public void processAndAssessorFilterTest() {
        levelBarGraph.setProcessFilter(Arrays.asList("SYS.5"));
        levelBarGraph.setAssessorFilter(Arrays.asList("Jakub"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"))
                .thenReturn(Arrays.asList("SYS.5", "SYS.4"));

        LinkedHashMap<String, LinkedHashMap<String, Integer>> dataMap = levelBarGraphService.getData(levelBarGraph);
        Assertions.assertEquals(1, dataMap.keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.5").size());
        Assertions.assertEquals(0, dataMap.get("SYS.5").get("Jakub"));
    }

    @Test
    @DisplayName("Non existing process filter.")
    public void nonExistingProcessFilterTest() {
        levelBarGraph.setProcessFilter(Arrays.asList("SYS.10"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"))
                .thenReturn(Arrays.asList("SYS.5", "SYS.4"));

        LinkedHashMap<String, LinkedHashMap<String, Integer>> dataMap = levelBarGraphService.getData(levelBarGraph);
        Assertions.assertEquals(1, dataMap.keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.10").keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.10").values().size());
        Assertions.assertEquals(0, dataMap.get("SYS.10").get("No measurements found."));
    }

    @Test
    @DisplayName("Non existing assessor filter.")
    public void nonExistingAssessorFilterTest() {
        levelBarGraph.setAssessorFilter(Arrays.asList("Wrong"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"))
                .thenReturn(Arrays.asList("SYS.5", "SYS.4"));

        LinkedHashMap<String, LinkedHashMap<String, Integer>> dataMap = levelBarGraphService.getData(levelBarGraph);
        Assertions.assertEquals(2, dataMap.keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.5").keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.4").keySet().size());
        Assertions.assertEquals(0, dataMap.get("SYS.4").get("No measurements found."));
        Assertions.assertEquals(0, dataMap.get("SYS.5").get("No measurements found."));
    }

    @Test
    @DisplayName("Non numeric score column.")
    public void badScoreColumnTest() {
        levelBarGraph.setScoreColumnName("Criterion");

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"))
                .thenReturn(Arrays.asList("SYS.5", "SYS.4"));

        Assertions.assertThrows(JasperReportException.class, () -> {
            levelBarGraphService.getData(levelBarGraph);
        });
    }

    @DisplayName("Aggregate scores.")
    @ParameterizedTest(name = "{index} - aggregate function: {0}")
    @EnumSource(names = {"AVG", "MAX", "MIN"})
    public void aggregateScoresTest(ScoreFunction scoreFunction) {
        levelBarGraph.setAggregateScoresFunction(scoreFunction);

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"))
                .thenReturn(Arrays.asList("SYS.5", "SYS.4"));

        LinkedHashMap<String, LinkedHashMap<String, Integer>> dataMap = levelBarGraphService.getData(levelBarGraph);
        Assertions.assertEquals(2, dataMap.keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.5").keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.4").keySet().size());

        if (scoreFunction.equals(ScoreFunction.MIN)) {
            Assertions.assertEquals(0, dataMap.get("SYS.4").values().iterator().next());
            Assertions.assertEquals(0, dataMap.get("SYS.5").values().iterator().next());
        } else if (scoreFunction.equals(ScoreFunction.MAX)) {
            Assertions.assertEquals(1, dataMap.get("SYS.5").values().iterator().next());
            Assertions.assertEquals(0, dataMap.get("SYS.4").values().iterator().next());
        } else if (scoreFunction.equals(ScoreFunction.AVG)) {
            Assertions.assertEquals(0, dataMap.get("SYS.5").values().iterator().next());
            Assertions.assertEquals(0, dataMap.get("SYS.4").values().iterator().next());
        } else {
            //Default is AVG
            Assertions.assertEquals(0, dataMap.get("SYS.5").values().iterator().next());
            Assertions.assertEquals(0, dataMap.get("SYS.4").values().iterator().next());
        }
    }

    @DisplayName("Aggregate levels.")
    @ParameterizedTest(name = "{index} - aggregate function: {0}")
    @EnumSource(names = {"MAX", "MIN"})
    public void aggregateLevelsTest(ScoreFunction scoreFunction) {
        levelBarGraph.setAggregateScoresFunction(scoreFunction);
        levelBarGraph.setAggregateLevels(true);

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"))
                .thenReturn(Arrays.asList("SYS.5", "SYS.4"));

        LinkedHashMap<String, LinkedHashMap<String, Integer>> dataMap = levelBarGraphService.getData(levelBarGraph);
        Assertions.assertEquals(2, dataMap.keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.5").keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.4").keySet().size());

        if (scoreFunction.equals(ScoreFunction.MIN)) {
            Assertions.assertEquals(0, dataMap.get("SYS.4").values().iterator().next());
            Assertions.assertEquals(0, dataMap.get("SYS.5").values().iterator().next());
        } else if (scoreFunction.equals(ScoreFunction.MAX)) {
            Assertions.assertEquals(1, dataMap.get("SYS.5").values().iterator().next());
            Assertions.assertEquals(0, dataMap.get("SYS.4").values().iterator().next());
        } else {
            //Default is MAX
            Assertions.assertEquals(1, dataMap.get("SYS.5").values().iterator().next());
            Assertions.assertEquals(0, dataMap.get("SYS.4").values().iterator().next());
        }
    }


    @DisplayName("Aggregate sources.")
    @ParameterizedTest(name = "{index} - aggregate function: {0}")
    @EnumSource(names = {"MAX", "MIN"})
    public void aggregateSourcesTest(ScoreFunction sourceFunction) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Source source2 = objectMapper.readValue(objectMapper.writeValueAsString(source), Source.class);
        source2.setSourceName("Source2");
        source2.getSourceColumns().get(4).setSourceData(
                Arrays.asList(
                        new SourceData("L"),
                        new SourceData("F"),
                        new SourceData("P"),
                        new SourceData("L"),
                        new SourceData("L"),
                        new SourceData("P"),
                        new SourceData("P"),
                        new SourceData("P"))
        );
        levelBarGraph.setSources(Arrays.asList(source, source2));
        levelBarGraph.setAggregateScoresFunction(ScoreFunction.MAX);
        levelBarGraph.setAggregateLevels(true);
        levelBarGraph.setAggregateSourcesFunction(sourceFunction);

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"))
                .thenReturn(Arrays.asList("SYS.5", "SYS.4"))
                .thenReturn(Arrays.asList("Tomas", "Jakub"))
                .thenReturn(Arrays.asList("SYS.5", "SYS.4"));

        LinkedHashMap<String, LinkedHashMap<String, Integer>> dataMap = levelBarGraphService.getData(levelBarGraph);
        Assertions.assertEquals(2, dataMap.keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.5").keySet().size());
        Assertions.assertEquals(1, dataMap.get("SYS.4").keySet().size());

        if (sourceFunction.equals(ScoreFunction.MIN)) {
            Assertions.assertEquals(0, dataMap.get("SYS.4").values().iterator().next());
            Assertions.assertEquals(1, dataMap.get("SYS.5").values().iterator().next());
        } else if (sourceFunction.equals(ScoreFunction.MAX)) {
            Assertions.assertEquals(1, dataMap.get("SYS.5").values().iterator().next());
            Assertions.assertEquals(1, dataMap.get("SYS.4").values().iterator().next());
        } else {
            //Default is MAX
            Assertions.assertEquals(1, dataMap.get("SYS.5").values().iterator().next());
            Assertions.assertEquals(0, dataMap.get("SYS.4").values().iterator().next());
        }
    }
}
