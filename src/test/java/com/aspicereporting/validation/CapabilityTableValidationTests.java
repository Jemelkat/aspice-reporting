package com.aspicereporting.validation;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.enums.ItemType;
import com.aspicereporting.entity.items.CapabilityTable;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.repository.SourceRepository;
import com.aspicereporting.service.ItemValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CapabilityTableValidationTests {
    @Mock
    private SourceRepository sourceRepository;
    @InjectMocks
    private ItemValidationService itemValidationService;

    private static CapabilityTable capabilityTable;
    private static Source source;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    public void beforeEach() {
        source = new Source();
        SourceColumn assessor = new SourceColumn(0L, "Assessor");
        SourceColumn process = new SourceColumn(1L, "Process");
        SourceColumn attribute = new SourceColumn(2L, "Level");
        SourceColumn criterion = new SourceColumn(3L, "Criterion");
        SourceColumn score = new SourceColumn(4L, "Score");

        List<SourceColumn> sourceColumns = Arrays.asList(process, assessor, attribute, criterion, score);
        source.addSourceColumns(sourceColumns);
        source.setSourceName("Source1");
        source.setId(1L);

        capabilityTable = new CapabilityTable();
        capabilityTable.setType(ItemType.CAPABILITY_TABLE);
        capabilityTable.setSource(source);
        capabilityTable.setAssessorColumn(new SourceColumn(0L, "Assessor"));
        capabilityTable.setProcessColumn(new SourceColumn(1L, "Process"));
        capabilityTable.setLevelColumn(new SourceColumn(2L, "Level"));
        capabilityTable.setCriterionColumn( new SourceColumn(3L, "Criterion"));
        capabilityTable.setScoreColumn(new SourceColumn(4L, "Score"));
    }

    @DisplayName("Valid item test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void validItemTest(boolean allowUndefined) {
        when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                .thenReturn(source);

        Assertions.assertDoesNotThrow(() -> itemValidationService.validateItem(capabilityTable, allowUndefined, new User()));
    }

    @DisplayName("Non existing source test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void notExistingSourceTest(boolean allowUndefined) {
        when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                .thenReturn(null);
        Assertions.assertThrows(EntityNotFoundException.class, () -> itemValidationService.validateItem(capabilityTable, allowUndefined, new User()));
    }

    @DisplayName("Missing source id test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void missingSourceTest(boolean allowUndefined) {
        source.setId(null);
        if (allowUndefined) {
            Assertions.assertDoesNotThrow(() -> itemValidationService.validateItem(capabilityTable, allowUndefined, new User()));
            Assertions.assertEquals(null, capabilityTable.getAssessorColumn());
            Assertions.assertEquals(null, capabilityTable.getProcessColumn());
            Assertions.assertEquals(null, capabilityTable.getLevelColumn());
            Assertions.assertEquals(null, capabilityTable.getCriterionColumn());
            Assertions.assertEquals(null, capabilityTable.getScoreColumn());
            Assertions.assertEquals(0, capabilityTable.getAssessorFilter().size());
        } else {
            Assertions.assertThrows(InvalidDataException.class, () -> itemValidationService.validateItem(capabilityTable, allowUndefined, new User()));
        }
    }

    @DisplayName("Missing ids test.")
    @ParameterizedTest()
    @ValueSource(ints = {1,2,3,4,5})
    public void missingColumnIdTest(int column) {
        when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                .thenReturn(source);
        switch (column) {
            case 1:
                capabilityTable.getAssessorColumn().setId(null);
                break;
            case 2:
                capabilityTable.getProcessColumn().setId(null);
                break;
            case 3:
                capabilityTable.getCriterionColumn().setId(null);
                break;
            case 4:
                capabilityTable.getLevelColumn().setId(null);
                break;
            case 5:
                capabilityTable.getScoreColumn().setId(null);
                break;
        }

        Assertions.assertThrows(EntityNotFoundException.class,() -> itemValidationService.validateItem(capabilityTable, true, new User()));
        Assertions.assertThrows(InvalidDataException.class,() -> itemValidationService.validateItem(capabilityTable, false, new User()));
    }

    @DisplayName("Source does not contain column.")
    @ParameterizedTest()
    @ValueSource(ints = {1,2,3,4,5})
    public void sourceMissingColumnsTest(int column) {
        when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                .thenReturn(source);
        switch (column) {
            case 1:
                capabilityTable.getAssessorColumn().setId(6L);
                break;
            case 2:
                capabilityTable.getProcessColumn().setId(6L);
                break;
            case 3:
                capabilityTable.getCriterionColumn().setId(6L);
                break;
            case 4:
                capabilityTable.getLevelColumn().setId(6L);
                break;
            case 5:
                capabilityTable.getScoreColumn().setId(6L);
                break;
        }

        Assertions.assertThrows(EntityNotFoundException.class,() -> itemValidationService.validateItem(capabilityTable, true, new User()));
        Assertions.assertThrows(EntityNotFoundException.class,() -> itemValidationService.validateItem(capabilityTable, false, new User()));
    }

    @DisplayName("Validator tests.")
    @ParameterizedTest(name = "{index} - {2}")
    @MethodSource("validatorInputs")
    public void validatorTests(CapabilityTable item, Integer violations, String testName) {
        Assertions.assertEquals(violations, validator.validate(item).size());
    }

    public static Stream<Arguments> validatorInputs() throws JsonProcessingException {
        capabilityTable = new CapabilityTable();
        capabilityTable.setType(ItemType.CAPABILITY_TABLE);
        capabilityTable.setSource(new Source());
        capabilityTable.setAssessorColumn(new SourceColumn(0L, "Assessor"));
        capabilityTable.setProcessColumn(new SourceColumn(1L, "Process"));
        capabilityTable.setLevelColumn(new SourceColumn(2L, "Level"));
        capabilityTable.setCriterionColumn( new SourceColumn(3L, "Criterion"));
        capabilityTable.setScoreColumn(new SourceColumn(4L, "Score"));

        ObjectMapper objectMapper = new ObjectMapper();
        CapabilityTable tableSource = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableSource.setSource(null);
        CapabilityTable tableAssessor = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableAssessor.setAssessorColumn(null);
        CapabilityTable tableProcess = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableProcess.setProcessColumn(null);
        CapabilityTable tableCriterion = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableCriterion.setCriterionColumn(null);
        CapabilityTable tableLevel = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableLevel.setLevelColumn(null);
        CapabilityTable tableScore = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableScore.setScoreColumn(null);
        CapabilityTable tableFunction = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableFunction.setAggregateScoresFunction(null);
        CapabilityTable tableFont = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableFont.setFontSize(-1);
        CapabilityTable tableProcessWidth = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableProcessWidth.setProcessWidth(-1);
        CapabilityTable tableCriterionWidth = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableCriterionWidth.setCriterionWidth(-1);
        CapabilityTable tableLevelMax = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableLevelMax.setLevelLimit(6);
        CapabilityTable tableLevelMin = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableLevelMin.setLevelLimit(0);
        CapabilityTable tableSpecificMax = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableSpecificMax.setLevelLimit(6);
        CapabilityTable tableSpecificMin = objectMapper.readValue(objectMapper.writeValueAsString(capabilityTable), CapabilityTable.class);
        tableSpecificMin.setLevelLimit(0);
        return Stream.of(
                arguments(capabilityTable, 0, "Valid item."),
                arguments(tableSource, 1, "Missing source."),
                arguments(tableAssessor, 1, "Missing assessor."),
                arguments(tableProcess, 1, "Missing process."),
                arguments(tableLevel, 1, "Missing level."),
                arguments(tableCriterion, 1, "Missing criterion."),
                arguments(tableScore, 1, "Missing score."),
                arguments(tableFunction, 1, "Missing function."),
                arguments(tableFont, 1, "Invalid font size."),
                arguments(tableProcessWidth, 1, "Invalid process width."),
                arguments(tableCriterionWidth, 1,  "Invalid criterion width."),
                arguments(tableLevelMax, 1,  "Max level bigger than 5."),
                arguments(tableLevelMin, 1, "Max level less than 1."),
                arguments(tableSpecificMax, 1, "Specific level bigger than 5."),
                arguments(tableSpecificMin, 1, "Specific level less than 1.")
        );

    }
}
