package com.aspicereporting.jasper;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.SourceData;
import com.aspicereporting.entity.enums.ScoreFunction;
import com.aspicereporting.entity.items.CapabilityTable;
import com.aspicereporting.exception.JasperReportException;
import com.aspicereporting.jasper.model.SimpleTableModel;
import com.aspicereporting.jasper.service.CapabilityTableService;
import com.aspicereporting.repository.SourceRepository;
import net.sf.jasperreports.components.table.StandardTable;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignComponentElement;
import net.sf.jasperreports.engine.design.JasperDesign;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
public class CapabilityTableTests {

    @Mock
    private SourceRepository sourceRepository;

    @InjectMocks
    private CapabilityTableService capabilityTableService;

    private static Source source;
    private CapabilityTable capabilityTable;

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

        SourceColumn attribute = new SourceColumn(2L, "Capability level");
        sd = new SourceData("Capability Level 1");
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        sd = new SourceData("Capability Level 2");
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        sd = new SourceData("Capability Level 1");
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
        score.addSourceData(new SourceData("F"));
        score.addSourceData(new SourceData("F"));
        score.addSourceData(new SourceData("L"));
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
        capabilityTable = new CapabilityTable();
        capabilityTable.setSource(source);
        capabilityTable.setAssessorColumn(source.getSourceColumns().get(0));
        capabilityTable.setProcessColumn(source.getSourceColumns().get(1));
        capabilityTable.setLevelColumn(source.getSourceColumns().get(2));
        capabilityTable.setCriterionColumn(source.getSourceColumns().get(3));
        capabilityTable.setScoreColumn(source.getSourceColumns().get(4));
    }


    @Test
    @DisplayName("Basic capability table.")
    public void basicObjectTest() {
        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));

        SimpleTableModel dataTable = capabilityTableService.getData(capabilityTable, new LinkedHashMap<>());
        Assertions.assertEquals(6, dataTable.getColumnCount());
        Assertions.assertEquals(2, dataTable.getRowCount());
    }

    @Test
    @DisplayName("Assessor filter - Tomas.")
    public void assessorFilterTest() {
        capabilityTable.setAssessorFilter(Arrays.asList("Tomas"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));


        SimpleTableModel dataTable = capabilityTableService.getData(capabilityTable, new LinkedHashMap<>());
        Assertions.assertEquals(6, dataTable.getColumnCount());
        Assertions.assertEquals(1, dataTable.getRowCount());
    }

    @Test
    @DisplayName("Assessor filter - Jakub.")
    public void assessorFilter2Test() {
        capabilityTable.setAssessorFilter(Arrays.asList("Jakub"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));


        SimpleTableModel dataTable = capabilityTableService.getData(capabilityTable, new LinkedHashMap<>());
        Assertions.assertEquals(4, dataTable.getColumnCount());
        Assertions.assertEquals(2, dataTable.getRowCount());
    }

    @Test
    @DisplayName("Non existing assessor filter.")
    public void nonExistingAssessorFilterTest() {
        capabilityTable.setAssessorFilter(Arrays.asList("Wrong"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));
        SimpleTableModel dataTable = capabilityTableService.getData(capabilityTable, new LinkedHashMap<>());
        Assertions.assertEquals(2, dataTable.getColumnCount());
        Assertions.assertEquals(0, dataTable.getRowCount());
    }

    @Test
    @DisplayName("Process filter.")
    public void processFilterTest() {
        capabilityTable.setProcessFilter(Arrays.asList("SYS.4"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));

        SimpleTableModel dataTable = capabilityTableService.getData(capabilityTable, new LinkedHashMap<>());
        Assertions.assertEquals(3, dataTable.getColumnCount());
        Assertions.assertEquals(1, dataTable.getRowCount());
    }

    @Test
    @DisplayName("Non existing process filter.")
    public void nonExistingProcessFilterTest() {
        capabilityTable.setAssessorFilter(Arrays.asList("Wrong"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));
        SimpleTableModel dataTable = capabilityTableService.getData(capabilityTable, new LinkedHashMap<>());
        Assertions.assertEquals(2, dataTable.getColumnCount());
        Assertions.assertEquals(0, dataTable.getRowCount());
    }

    @Test
    @DisplayName("Process and assessor filter.")
    public void processAndAssessorFilterTest() {
        capabilityTable.setProcessFilter(Arrays.asList("SYS.5"));
        capabilityTable.setAssessorFilter(Arrays.asList("Jakub"));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"))
                .thenReturn(Arrays.asList("SYS.5", "SYS.4"));

        SimpleTableModel dataTable = capabilityTableService.getData(capabilityTable, new LinkedHashMap<>());
        Assertions.assertEquals(4, dataTable.getColumnCount());
        Assertions.assertEquals(1, dataTable.getRowCount());
    }

    @Test
    @DisplayName("Non numeric score column.")
    public void badScoreColumnTest() {
        capabilityTable.setScoreColumn(source.getSourceColumns().get(0));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));

        Assertions.assertThrows(JasperReportException.class, () -> {
            capabilityTableService.getData(capabilityTable, new LinkedHashMap<>());
        });
    }

    @Test
    @DisplayName("Non numeric score column.")
    public void maxLevelValidTest() {
        capabilityTable.setScoreColumn(source.getSourceColumns().get(0));

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));

        Assertions.assertThrows(JasperReportException.class, () -> {
            capabilityTableService.getData(capabilityTable, new LinkedHashMap<>());
        });
    }

    @DisplayName("Aggregate scores.")
    @ParameterizedTest(name = "{index} - aggregate function: {0}")
    @EnumSource(names = {"AVG", "MAX", "MIN"})
    public void aggregateScoresTest(ScoreFunction scoreFunction) {
        capabilityTable.setAggregateScoresFunction(scoreFunction);

        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));

        SimpleTableModel dataTable = capabilityTableService.getData(capabilityTable, new LinkedHashMap<>());

        Assertions.assertEquals(6, dataTable.getColumnCount());
        Assertions.assertEquals(2, dataTable.getRowCount());

        String[] expectedSys4 = {"SYS.4", "L", "L", "", "", ""};
        String[] expectedSys5;
        if (scoreFunction.equals(ScoreFunction.MIN)) {
            expectedSys5 = new String[]{"SYS.5", "P", "P", "L", "L", "L", "L", "L"};
        } else if (scoreFunction.equals(ScoreFunction.MAX)) {
            expectedSys5 = new String[]{"SYS.5", "F", "F", "L", "L", "L", "L", "L"};
        } else {
            expectedSys5 = new String[]{"SYS.5", "L", "L", "L", "L", "L", "L", "L"};
        }

        for (int col = 0; col < 6; col++) {
            Assertions.assertEquals(expectedSys4[col], dataTable.getValueAt(0, col));
            Assertions.assertEquals(expectedSys5[col], dataTable.getValueAt(1, col));
        }
    }

    @ParameterizedTest(name = "{index} - maxlevel: {0}")
    @DisplayName("Max levels tests")
    @MethodSource("maxLevelsInputs")
    public void maxLevelsTest(int maxLevel, int expectedColumns, List<String> criterions) {
        capabilityTable.setLevelLimit(maxLevel);
        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));

        SimpleTableModel dataTable = capabilityTableService.getData(capabilityTable, new LinkedHashMap<>());
        Assertions.assertEquals(expectedColumns, dataTable.getColumnCount());
        for (int col = 0; col < expectedColumns; col++) {
            Assertions.assertEquals(criterions.get(col), dataTable.getColumnName(col));
        }
    }


    @ParameterizedTest(name = "{index} - specific level: {0}")
    @DisplayName("Specific levels tests")
    @MethodSource("specificLevelInputs")
    public void specificLevelTest(int maxLevel, int expectedColumns, List<String> criterions) {
        capabilityTable.setSpecificLevel(maxLevel);
        Mockito.when(sourceRepository.findDistinctColumnValuesForColumn(Mockito.any(Long.class)))
                .thenReturn(Arrays.asList("Tomas", "Jakub"));

        SimpleTableModel dataTable = capabilityTableService.getData(capabilityTable, new LinkedHashMap<>());
        Assertions.assertEquals(expectedColumns, dataTable.getColumnCount());
        for (int col = 0; col < expectedColumns; col++) {
            Assertions.assertEquals(criterions.get(col), dataTable.getColumnName(col));
        }
    }

    @DisplayName("Create capability table element test")
    @Test
    public void createElementTest() throws JRException {
        JasperDesign jasperDesign = new JasperDesign();
        Map<String, Object> parameters =  new HashMap<>();
        JRDesignComponentElement element = capabilityTableService.createElement(jasperDesign,capabilityTable,0, parameters);

        Assertions.assertEquals("tableDataset0", ((StandardTable)element.getComponent()).getDatasetRun().getDatasetName());
        Assertions.assertEquals(3, ((StandardTable)element.getComponent()).getColumns().size());
        Assertions.assertEquals(6, jasperDesign.getDatasetMap().get("tableDataset0").getFields().length);
        Assertions.assertEquals(1, parameters.size());
        Assertions.assertTrue(parameters.keySet().contains("tableData0"));
    }

    public static Stream<Arguments> maxLevelsInputs() {
        return Stream.of(
                arguments(1, 4, Arrays.asList("Process Name", "BP1", "BP2", "BP3")),
                arguments(2, 6, Arrays.asList("Process Name", "BP1", "BP2", "BP3", "2.1", "2.2")),
                arguments(6, 6, Arrays.asList("Process Name", "BP1", "BP2", "BP3", "2.1", "2.2")),
                arguments(0, 2, Arrays.asList("Process Name", "No criterions found"))
        );
    }

    public static Stream<Arguments> specificLevelInputs() {
        return Stream.of(
                arguments(1, 4, Arrays.asList("Process Name", "BP1", "BP2", "BP3")),
                arguments(2, 3, Arrays.asList("Process Name", "2.1", "2.2")),
                arguments(6, 2, Arrays.asList("Process Name", "No criterions found"))
        );
    }
}
