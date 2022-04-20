package com.aspicereporting.validation;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.enums.ItemType;
import com.aspicereporting.entity.items.SimpleTable;
import com.aspicereporting.entity.items.TableColumn;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.repository.SourceRepository;
import com.aspicereporting.service.ItemValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SimpleTableValidationTests {
    @Mock
    private SourceRepository sourceRepository;
    @InjectMocks
    private ItemValidationService itemValidationService;

    private static SimpleTable simpleTable;
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

        simpleTable = new SimpleTable();
        simpleTable.setType(ItemType.SIMPLE_TABLE);
        simpleTable.setSource(source);
        TableColumn tc1 = new TableColumn();
        tc1.setSourceColumn(new SourceColumn(0L, "Assessor"));
        TableColumn tc2 = new TableColumn();
        tc2.setSourceColumn(new SourceColumn(1L, "Process"));
        simpleTable.setTableColumns(Arrays.asList(tc1, tc2));
    }

    @DisplayName("Valid item test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void validItemTest(boolean allowUndefined) {
        when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                .thenReturn(source);

        Assertions.assertDoesNotThrow(() -> itemValidationService.validateItem(simpleTable, allowUndefined, new User()));
    }

    @DisplayName("Empty columns test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void emptyColumnsTest(boolean allowUndefined) {
        simpleTable.setTableColumns(new ArrayList<>());
        if (allowUndefined) {
            when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                    .thenReturn(source);
            Assertions.assertDoesNotThrow(() -> itemValidationService.validateItem(simpleTable, allowUndefined, new User()));
        } else {
            Assertions.assertThrows(InvalidDataException.class, () -> itemValidationService.validateItem(simpleTable, allowUndefined, new User()));

        }
    }

    @DisplayName("Non existing source test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void notExistingSourceTest(boolean allowUndefined) {
        when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                .thenReturn(null);
        Assertions.assertThrows(EntityNotFoundException.class, () -> itemValidationService.validateItem(simpleTable, allowUndefined, new User()));
    }

    @DisplayName("Missing source id test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void missingSourceTest(boolean allowUndefined) {
        source.setId(null);
        if (allowUndefined) {
            Assertions.assertDoesNotThrow(() -> itemValidationService.validateItem(simpleTable, allowUndefined, new User()));
            for (TableColumn tc : simpleTable.getTableColumns()) {
                Assertions.assertEquals(null, tc.getSourceColumn());
            }
        } else {
            Assertions.assertThrows(InvalidDataException.class, () -> itemValidationService.validateItem(simpleTable, allowUndefined, new User()));
        }
    }

    @DisplayName("Missing column id test.")
    @Test
    public void missingColumnIdTest() {
        when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                .thenReturn(source);
        simpleTable.getTableColumns().get(0).getSourceColumn().setId(null);

        Assertions.assertThrows(EntityNotFoundException.class, () -> itemValidationService.validateItem(simpleTable, true, new User()));
        Assertions.assertThrows(InvalidDataException.class, () -> itemValidationService.validateItem(simpleTable, false, new User()));
    }

    @DisplayName("Source does not contain column.")
    @Test
    public void sourceMissingColumnsTest() {
        when(sourceRepository.findByIdAndUserOrSourceGroupsIn(any(Long.class), any(User.class), any(Set.class)))
                .thenReturn(source);
        simpleTable.getTableColumns().get(0).getSourceColumn().setId(6L);

        Assertions.assertThrows(EntityNotFoundException.class, () -> itemValidationService.validateItem(simpleTable, true, new User()));
        Assertions.assertThrows(EntityNotFoundException.class, () -> itemValidationService.validateItem(simpleTable, false, new User()));
    }

    @DisplayName("Validator tests.")
    @ParameterizedTest(name = "{index} - {2}")
    @MethodSource("validatorInputs")
    public void validatorTests(SimpleTable item, Integer violations, String testName) {
        Assertions.assertEquals(violations, validator.validate(item).size());
    }

    public static Stream<Arguments> validatorInputs() throws JsonProcessingException {
        simpleTable = new SimpleTable();
        simpleTable.setType(ItemType.SIMPLE_TABLE);
        simpleTable.setSource(new Source());
        TableColumn tc1 = new TableColumn();
        tc1.setSourceColumn(new SourceColumn(0L, "Assessor"));
        TableColumn tc2 = new TableColumn();
        tc2.setSourceColumn(new SourceColumn(1L, "Process"));
        simpleTable.setTableColumns(Arrays.asList(tc1, tc2));

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleTable tableSource = objectMapper.readValue(objectMapper.writeValueAsString(simpleTable), SimpleTable.class);
        tableSource.setSource(null);
        SimpleTable tableColumns = objectMapper.readValue(objectMapper.writeValueAsString(simpleTable), SimpleTable.class);
        tableColumns.getTableColumns().clear();
        SimpleTable tableColumns2 = objectMapper.readValue(objectMapper.writeValueAsString(simpleTable), SimpleTable.class);
        tableColumns2.setTableColumns(null);
        SimpleTable tableColumnSource = objectMapper.readValue(objectMapper.writeValueAsString(simpleTable), SimpleTable.class);
        tableColumnSource.getTableColumns().get(0).setSourceColumn(null);
        SimpleTable tableColumnWidth = objectMapper.readValue(objectMapper.writeValueAsString(simpleTable), SimpleTable.class);
        tableColumnWidth.getTableColumns().get(0).setWidth(0);
        return Stream.of(
                arguments(simpleTable, 0, "Valid item."),
                arguments(tableSource, 1, "Missing source."),
                arguments(tableColumns, 1, "Missing columns."),
                arguments(tableColumns2, 1, "Missing columns null."),
                arguments(tableColumnSource, 1, "Missing column source."),
                arguments(tableColumnWidth, 1, "Column width less than 1.")
        );
    }
}
