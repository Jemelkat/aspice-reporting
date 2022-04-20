package com.aspicereporting.validation;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.enums.ItemType;
import com.aspicereporting.entity.items.SimpleTable;
import com.aspicereporting.entity.items.TableColumn;
import com.aspicereporting.entity.items.TextItem;
import com.aspicereporting.entity.items.TextStyle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class TextValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @DisplayName("Validator tests.")
    @ParameterizedTest(name = "{index} - {2}")
    @MethodSource("validatorInputs")
    public void validatorTests(TextItem item, Integer violations, String testName) {
        Assertions.assertEquals(violations, validator.validate(item).size());
    }

    public static Stream<Arguments> validatorInputs() throws JsonProcessingException {
        TextItem text = new TextItem();
        text.setType(ItemType.TEXT);
        TextStyle style = new TextStyle();
        text.setTextStyle(style);

        ObjectMapper objectMapper = new ObjectMapper();
        TextItem textFont = objectMapper.readValue(objectMapper.writeValueAsString(text), TextItem.class);
        textFont.getTextStyle().setFontSize(0);
        TextItem textColor = objectMapper.readValue(objectMapper.writeValueAsString(text), TextItem.class);
        textColor.getTextStyle().setColor("INVALID");

        return Stream.of(
                arguments(text, 0, "Valid item."),
                arguments(textFont, 1, "Wrong font size 0."),
                arguments(textColor, 1, "Invalid color hex value.")
        );
    }

}
