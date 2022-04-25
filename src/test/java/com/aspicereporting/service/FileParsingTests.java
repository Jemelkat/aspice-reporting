package com.aspicereporting.service;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.SourceData;
import com.aspicereporting.exception.SourceFileException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileParsingTests {

    private static FileParsingService fileParsingService;

    @BeforeAll
    public static void init() {
        fileParsingService = new FileParsingService();
    }

    @ParameterizedTest(name = "Input: ''{0}''")
    @ValueSource(strings = {"validFile.csv", "validFile.xls", "validFile.xlsx"})
    public void parseValidFiles(String fileName) throws IOException {
        InputStream stream;
        MockMultipartFile multipartFile;
        File file = new File("src/test/resources/files/" + fileName);
        stream = new FileInputStream(file);
        multipartFile = new MockMultipartFile("file", file.getName(),
                String.valueOf(MediaType.MULTIPART_FORM_DATA), stream);
        Source source = fileParsingService.parseFile(multipartFile);

        Assertions.assertEquals(11, source.getSourceColumns().size());
        for (var sourceColumn : source.getSourceColumns()) {
            Assertions.assertEquals(28, sourceColumn.getSourceData().size());
        }
    }

    @ParameterizedTest(name = "Input: ''{0}''")
    @ValueSource(strings = {"emptyFile.csv", "emptyFile.xls", "emptyFile.xlsx"})
    public void parseEmptyFiles(String fileName) throws IOException {
        InputStream stream;
        MockMultipartFile multipartFile;
        File file = new File("src/test/resources/files/" + fileName);
        stream = new FileInputStream(file);
        multipartFile = new MockMultipartFile("file", file.getName(),
                String.valueOf(MediaType.MULTIPART_FORM_DATA), stream);
        Assertions.assertThrows(SourceFileException.class, () -> fileParsingService.parseFile(multipartFile));
    }

    @Test
    public void parseNullFile() {
        SourceFileException exception = Assertions.assertThrows(SourceFileException.class, () -> fileParsingService.parseFile(null));
        Assertions.assertEquals("Cannot read null file.", exception.getMessage());
    }


    @Test
    public void parseUnsupportedFile() throws IOException {
        InputStream stream;
        MockMultipartFile multipartFile;
        File file = new File("src/test/resources/files/unsupported.pdf");
        stream = new FileInputStream(file);
        multipartFile = new MockMultipartFile("file", file.getName(),
                String.valueOf(MediaType.MULTIPART_FORM_DATA), stream);
        Assertions.assertThrows(SourceFileException.class, () -> fileParsingService.parseFile(multipartFile));
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("sourcesProvider")
    public void createCSV(String name, Source source, int size) {
        ByteArrayOutputStream outputStream = fileParsingService.parseSourceToCSV(source);
        Assertions.assertNotNull(outputStream);
        Assertions.assertEquals(size, outputStream.size());
    }

    private static Stream<Arguments> sourcesProvider() {
        Source emptySource = new Source();
        Source filledSource = new Source();

        SourceColumn sourceColumn1 = new SourceColumn();
        sourceColumn1.setColumnName("Column1");
        SourceData sourceData1 = new SourceData();
        sourceData1.setValue("1");
        SourceData sourceData2 = new SourceData();
        sourceData2.setValue("2");
        SourceData sourceData3 = new SourceData();
        sourceData3.setValue("3");
        sourceColumn1.addSourceData(sourceData1);
        sourceColumn1.addSourceData(sourceData2);
        sourceColumn1.addSourceData(sourceData3);

        SourceColumn sourceColumn2 = new SourceColumn();
        sourceColumn2.setColumnName("Column2");
        SourceData sourceData4 = new SourceData();
        sourceData4.setValue("1");
        SourceData sourceData5 = new SourceData();
        sourceData5.setValue("2");
        SourceData sourceData6 = new SourceData();
        sourceData6.setValue("3");

        sourceColumn2.addSourceData(sourceData4);
        sourceColumn2.addSourceData(sourceData5);
        sourceColumn2.addSourceData(sourceData6);

        filledSource.addSourceColumns(Stream.of(sourceColumn1, sourceColumn2).collect(Collectors.toList()));

        return Stream.of(
                Arguments.of("Empty source", emptySource, 0),
                Arguments.of("Filled source", filledSource, 28),
                Arguments.of("Null source", null, 0)
        );
    }
}
