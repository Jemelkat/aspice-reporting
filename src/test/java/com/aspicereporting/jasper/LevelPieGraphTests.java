package com.aspicereporting.jasper;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.SourceData;
import com.aspicereporting.entity.enums.ScoreFunction;
import com.aspicereporting.entity.items.LevelPieGraph;
import com.aspicereporting.exception.JasperReportException;
import com.aspicereporting.jasper.service.LevelPieGraphService;
import com.aspicereporting.repository.SourceRepository;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JasperDesign;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

@ExtendWith(MockitoExtension.class)
public class LevelPieGraphTests {

    @Mock
    private SourceRepository sourceRepository;

    @InjectMocks
    private LevelPieGraphService levelPieGraphService;

    private static Source source;
    private LevelPieGraph levelPieGraph;

    @BeforeAll
    public static void init() {
        source = new Source();

        SourceColumn assessor = new SourceColumn(0L, "Assessor");
        SourceData sd = new SourceData("Tomas");
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
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
        process.addSourceData(sd);
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
        sd = new SourceData("PA2.1");
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        sd = new SourceData("PA2.2");
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        sd = new SourceData("PA1.1");
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);

        SourceColumn criterion = new SourceColumn(3L, "Criterion");
        criterion.addSourceData(new SourceData("BP1"));
        criterion.addSourceData(new SourceData("BP2"));
        criterion.addSourceData(new SourceData("BP3"));
        criterion.addSourceData(new SourceData("2.1"));
        criterion.addSourceData(new SourceData("2.1"));
        criterion.addSourceData(new SourceData("2.2"));
        criterion.addSourceData(new SourceData("2.2"));
        criterion.addSourceData(new SourceData("BP1"));
        criterion.addSourceData(new SourceData("BP2"));
        criterion.addSourceData(new SourceData("BP1"));
        criterion.addSourceData(new SourceData("BP2"));
        criterion.addSourceData(new SourceData("BP3"));

        SourceColumn score = new SourceColumn(4L, "Score");
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("F"));
        score.addSourceData(new SourceData("F"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("P"));
        score.addSourceData(new SourceData("P"));
        score.addSourceData(new SourceData("L"));

        List<SourceColumn> sourceColumns = Arrays.asList(assessor, process, attribute, criterion, score);
        source.addSourceColumns(sourceColumns);
        source.setSourceName("Source1");
    }

    @BeforeEach
    public void beforeEach() {
        levelPieGraph = new LevelPieGraph();
        levelPieGraph.setSource(source);
        levelPieGraph.setAssessorColumn(source.getSourceColumns().get(0));
        levelPieGraph.setProcessColumn(source.getSourceColumns().get(1));
        levelPieGraph.setAttributeColumn(source.getSourceColumns().get(2));
        levelPieGraph.setCriterionColumn(source.getSourceColumns().get(3));
        levelPieGraph.setScoreColumn(source.getSourceColumns().get(4));
    }

    @Test
    @DisplayName("Assessor filter - Tomas.")
    public void assessorFilterTest() {
        levelPieGraph.setAssessorFilter(Arrays.asList("Tomas"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));


        LinkedHashMap<String, Integer> dataMap = levelPieGraphService.getData(levelPieGraph);
        Assertions.assertEquals(1, dataMap.keySet().size());
        Assertions.assertEquals(1, dataMap.get("Level 2"));
    }

    @Test
    @DisplayName("Assessor filter - Jakub.")
    public void assessorFilter2Test() {
        levelPieGraph.setAssessorFilter(Arrays.asList("Jakub"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));


        LinkedHashMap<String, Integer> dataMap = levelPieGraphService.getData(levelPieGraph);
        Assertions.assertEquals(2, dataMap.keySet().size());
        Assertions.assertEquals(1, dataMap.get("Level 0"));
        Assertions.assertEquals(1, dataMap.get("Level 1"));
    }

    @Test
    @DisplayName("Non existing assessor filter.")
    public void nonExistingAssessorFilterTest() {
        levelPieGraph.setAssessorFilter(Arrays.asList("Wrong"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));

        LinkedHashMap<String, Integer> dataMap = levelPieGraphService.getData(levelPieGraph);
        Assertions.assertEquals(1, dataMap.keySet().size());
        Assertions.assertEquals(1, dataMap.get("No measurements found"));
    }

    @Test
    @DisplayName("Non numeric score column.")
    public void badScoreColumnTest() {
        levelPieGraph.setScoreColumn(source.getSourceColumns().get(0));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));

        Assertions.assertThrows(JasperReportException.class, () -> {
            levelPieGraphService.getData(levelPieGraph);
        });
    }

    @DisplayName("Aggregate scores.")
    @ParameterizedTest(name = "{index} - aggregate function: {0}")
    @EnumSource(names = {"AVG", "MAX", "MIN"})
    public void aggregateScoresTest(ScoreFunction scoreFunction) {
        levelPieGraph.setAggregateScoresFunction(scoreFunction);

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));

        LinkedHashMap<String, Integer> dataMap = levelPieGraphService.getData(levelPieGraph);
        if (scoreFunction.equals(ScoreFunction.MIN)) {
            Assertions.assertEquals(2, dataMap.keySet().size());
            Assertions.assertEquals(1, dataMap.get("Level 0"));
            Assertions.assertEquals(1, dataMap.get("Level 1"));
        } else if (scoreFunction.equals(ScoreFunction.MAX)) {
            Assertions.assertEquals(2, dataMap.keySet().size());
            Assertions.assertEquals(1, dataMap.get("Level 2"));
            Assertions.assertEquals(1, dataMap.get("Level 1"));
        } else {
            Assertions.assertEquals(1, dataMap.keySet().size());
            Assertions.assertEquals(2, dataMap.get("Level 1"));
        }
    }

    @DisplayName("Aggregate levels.")
    @ParameterizedTest(name = "{index} - aggregate function: {0}")
    @EnumSource(names = {"MAX", "MIN"})
    public void aggregateLevelsTest(ScoreFunction scoreFunction) {
        levelPieGraph.setAggregateScoresFunction(scoreFunction);
        levelPieGraph.setAggregateLevels(true);

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));

        LinkedHashMap<String, Integer> dataMap = levelPieGraphService.getData(levelPieGraph);
        if (scoreFunction.equals(ScoreFunction.MIN)) {
            Assertions.assertEquals(2, dataMap.keySet().size());
            Assertions.assertEquals(1, dataMap.get("Level 0"));
            Assertions.assertEquals(1, dataMap.get("Level 1"));
        } else {
            //Default is MAX
            Assertions.assertEquals(2, dataMap.keySet().size());
            Assertions.assertEquals(1, dataMap.get("Level 2"));
            Assertions.assertEquals(1, dataMap.get("Level 1"));
        }
    }

    @DisplayName("Create pie graph element test")
    @Test
    public void createElementTest() throws JRException {
        JasperDesign jasperDesign = new JasperDesign();
        Map<String, Object> parameters =  new HashMap<>();
        JRDesignImage jrDesignImage = levelPieGraphService.createElement(jasperDesign,levelPieGraph,0, parameters);

        Assertions.assertNotNull(jrDesignImage);
        Assertions.assertEquals(1, parameters.keySet().size());
        Assertions.assertTrue(jasperDesign.getParametersMap().containsKey("chart0"));
    }
}
