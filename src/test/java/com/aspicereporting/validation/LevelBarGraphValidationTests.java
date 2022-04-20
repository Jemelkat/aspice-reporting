package com.aspicereporting.validation;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.SourceData;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.items.LevelBarGraph;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.repository.SourceRepository;
import com.aspicereporting.service.ItemValidationService;
import net.bytebuddy.description.method.ParameterDescription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LevelBarGraphValidationTests {

    @Mock
    private SourceRepository sourceRepository;
    @InjectMocks
    private ItemValidationService itemValidationService;

    private static LevelBarGraph levelBarGraph;
    private static Source source;

    @BeforeAll
    public static void initAll() {
        source = new Source();
        SourceColumn assessor = new SourceColumn(0L, "Assessor");
        SourceColumn process = new SourceColumn(1L, "Process");
        SourceColumn attribute = new SourceColumn(2L, "Attribute");
        SourceColumn criterion = new SourceColumn(3L, "Criterion");
        SourceColumn score = new SourceColumn(4L, "Score");

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

    @DisplayName("Valid item test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void validItemTest(boolean allowUndefined) {
        when(sourceRepository.findByIdInAndUserOrSourceGroupsIn(any(Set.class), any(User.class), any(Set.class)))
                .thenReturn(Arrays.asList(source));

        Assertions.assertDoesNotThrow(() -> itemValidationService.validateItemWithValid(levelBarGraph, allowUndefined, new User()));
    }

    @DisplayName("Missing source test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void missingSourceTest(boolean allowUndefined) {
        levelBarGraph.setSources(new ArrayList<>());
        if(allowUndefined) {
            Assertions.assertDoesNotThrow(() -> itemValidationService.validateItemWithValid(levelBarGraph, allowUndefined, new User()));
            Assertions.assertEquals(null, levelBarGraph.getAssessorColumnName());
            Assertions.assertEquals(null, levelBarGraph.getProcessColumnName());
            Assertions.assertEquals(null, levelBarGraph.getAttributeColumnName());
            Assertions.assertEquals(null, levelBarGraph.getScoreColumnName());
        } else {
            Assertions.assertThrows(InvalidDataException.class,() -> itemValidationService.validateItemWithValid(levelBarGraph, allowUndefined, new User()));
        }
    }

    @DisplayName("Inaccessible source test.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void inaccessibleSourceTest(boolean allowUndefined) {
        when(sourceRepository.findByIdInAndUserOrSourceGroupsIn(any(Set.class), any(User.class), any(Set.class)))
                .thenReturn(new ArrayList());

        Assertions.assertThrows(InvalidDataException.class,() -> itemValidationService.validateItemWithValid(levelBarGraph, allowUndefined, new User()));
    }
}
