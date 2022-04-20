package com.aspicereporting.validation;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.enums.ItemType;
import com.aspicereporting.entity.items.LevelBarGraph;
import com.aspicereporting.entity.items.LevelPieGraph;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.repository.SourceRepository;
import com.aspicereporting.service.ItemValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LevelPieGraphValidationTests {

    @Mock
    private SourceRepository sourceRepository;
    @InjectMocks
    private ItemValidationService itemValidationService;

    private static LevelPieGraph levelPieGraph;
    private static Source source;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    public void beforeEach() {
        source = new Source();
        SourceColumn assessor = new SourceColumn(0L, "Assessor");
        SourceColumn process = new SourceColumn(1L, "Process");
        SourceColumn attribute = new SourceColumn(2L, "Attribute");
        SourceColumn criterion = new SourceColumn(3L, "Criterion");
        SourceColumn score = new SourceColumn(4L, "Score");

        List<SourceColumn> sourceColumns = Arrays.asList(process, assessor, attribute, criterion, score);
        source.addSourceColumns(sourceColumns);
        source.setSourceName("Source1");
        source.setId(1L);

        levelPieGraph = new LevelPieGraph();
        levelPieGraph.setType(ItemType.LEVEL_PIE_GRAPH);
        levelPieGraph.setSource(source);
        levelPieGraph.setAssessorColumn(new SourceColumn(0L, "Assessor"));
        levelPieGraph.setProcessColumn(new SourceColumn(1L, "Process"));
        levelPieGraph.setAttributeColumn(new SourceColumn(2L, "Attribute"));
        levelPieGraph.setCriterionColumn( new SourceColumn(3L, "Criterion"));
        levelPieGraph.setScoreColumn(new SourceColumn(4L, "Score"));
    }


    @DisplayName("Valid item test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void validItemTest(boolean allowUndefined) {
        when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                .thenReturn(source);

        Assertions.assertDoesNotThrow(() -> itemValidationService.validateItem(levelPieGraph, allowUndefined, new User()));
    }

    @DisplayName("Non existing source test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void notExistingSourceTest(boolean allowUndefined) {
        when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                .thenReturn(null);
        Assertions.assertThrows(EntityNotFoundException.class, () -> itemValidationService.validateItem(levelPieGraph, allowUndefined, new User()));
    }

    @DisplayName("Missing source id test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void missingSourceTest(boolean allowUndefined) {
        source.setId(null);
        if (allowUndefined) {
            Assertions.assertDoesNotThrow(() -> itemValidationService.validateItem(levelPieGraph, allowUndefined, new User()));
            Assertions.assertEquals(null, levelPieGraph.getAssessorColumn());
            Assertions.assertEquals(null, levelPieGraph.getProcessColumn());
            Assertions.assertEquals(null, levelPieGraph.getAttributeColumn());
            Assertions.assertEquals(null, levelPieGraph.getCriterionColumn());
            Assertions.assertEquals(null, levelPieGraph.getScoreColumn());
            Assertions.assertEquals(0, levelPieGraph.getAssessorFilter().size());
        } else {
            Assertions.assertThrows(InvalidDataException.class, () -> itemValidationService.validateItem(levelPieGraph, allowUndefined, new User()));
        }
    }

    @DisplayName("Missing ids test.")
    @ParameterizedTest()
    @ValueSource(ints = {1,2,3,4,5})
    public void missingSourceTest(int column) {
        when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                .thenReturn(source);
        switch (column) {
            case 1:
                levelPieGraph.getAssessorColumn().setId(null);
                break;
            case 2:
                levelPieGraph.getProcessColumn().setId(null);
                break;
            case 3:
                levelPieGraph.getCriterionColumn().setId(null);
                break;
            case 4:
                levelPieGraph.getAttributeColumn().setId(null);
                break;
            case 5:
                levelPieGraph.getScoreColumn().setId(null);
                break;
        }

        Assertions.assertThrows(EntityNotFoundException.class,() -> itemValidationService.validateItem(levelPieGraph, true, new User()));
        Assertions.assertThrows(InvalidDataException.class,() -> itemValidationService.validateItem(levelPieGraph, false, new User()));
    }

    @DisplayName("Source does not contain column.")
    @ParameterizedTest()
    @ValueSource(ints = {1,2,3,4,5})
    public void sourceMissingColumnsTest(int column) {
        when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                .thenReturn(source);
        switch (column) {
            case 1:
                levelPieGraph.getAssessorColumn().setId(6L);
                break;
            case 2:
                levelPieGraph.getProcessColumn().setId(6L);
                break;
            case 3:
                levelPieGraph.getCriterionColumn().setId(6L);
                break;
            case 4:
                levelPieGraph.getAttributeColumn().setId(6L);
                break;
            case 5:
                levelPieGraph.getScoreColumn().setId(6L);
                break;
        }

        Assertions.assertThrows(EntityNotFoundException.class,() -> itemValidationService.validateItem(levelPieGraph, true, new User()));
        Assertions.assertThrows(EntityNotFoundException.class,() -> itemValidationService.validateItem(levelPieGraph, false, new User()));
    }


    @DisplayName("Validator tests.")
    @ParameterizedTest(name = "{index} - {2}")
    @MethodSource("validatorInputs")
    public void validatorTests(LevelPieGraph item, Integer violations, String testName) {
        Assertions.assertEquals(violations, validator.validate(item).size());
    }

    public static Stream<Arguments> validatorInputs() throws JsonProcessingException {
        levelPieGraph = new LevelPieGraph();
        levelPieGraph.setSource(source);
        levelPieGraph.setType(ItemType.LEVEL_PIE_GRAPH);
        levelPieGraph.setAssessorColumn(source.getSourceColumns().get(0));
        levelPieGraph.setProcessColumn(source.getSourceColumns().get(1));
        levelPieGraph.setAttributeColumn(source.getSourceColumns().get(2));
        levelPieGraph.setCriterionColumn(source.getSourceColumns().get(3));
        levelPieGraph.setScoreColumn(source.getSourceColumns().get(4));

        ObjectMapper objectMapper = new ObjectMapper();
        LevelPieGraph levelPieGraphSource = objectMapper.readValue(objectMapper.writeValueAsString(levelPieGraph), LevelPieGraph.class);
        levelPieGraphSource.setSource(null);
        LevelPieGraph levelPieGraphAssessor = objectMapper.readValue(objectMapper.writeValueAsString(levelPieGraph), LevelPieGraph.class);
        levelPieGraphAssessor.setAssessorColumn(null);
        LevelPieGraph levelPieGraphProcess = objectMapper.readValue(objectMapper.writeValueAsString(levelPieGraph), LevelPieGraph.class);
        levelPieGraphProcess.setProcessColumn(null);
        LevelPieGraph levelPieGraphAttribute = objectMapper.readValue(objectMapper.writeValueAsString(levelPieGraph), LevelPieGraph.class);
        levelPieGraphAttribute.setAttributeColumn(null);
        LevelPieGraph levelPieGraphCriterion = objectMapper.readValue(objectMapper.writeValueAsString(levelPieGraph), LevelPieGraph.class);
        levelPieGraphCriterion.setCriterionColumn(null);
        LevelPieGraph levelPieGraphScore = objectMapper.readValue(objectMapper.writeValueAsString(levelPieGraph), LevelPieGraph.class);
        levelPieGraphScore.setScoreColumn(null);
        LevelPieGraph levelPieGraphFunction = objectMapper.readValue(objectMapper.writeValueAsString(levelPieGraph), LevelPieGraph.class);
        levelPieGraphFunction.setAggregateScoresFunction(null);
        return Stream.of(
                arguments(levelPieGraph, 0, "Valid item."),
                arguments(levelPieGraphSource, 1, "Missing source."),
                arguments(levelPieGraphAssessor, 1, "Missing assessor."),
                arguments(levelPieGraphProcess, 1, "Missing process."),
                arguments(levelPieGraphAttribute, 1, "Missing attribute."),
                arguments(levelPieGraphCriterion, 1, "Missing criterion."),
                arguments(levelPieGraphScore, 1, "Missing score."),
                arguments(levelPieGraphFunction, 1, "Missing function.")
        );

    }
}
