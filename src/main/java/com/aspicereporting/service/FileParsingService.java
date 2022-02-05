package com.aspicereporting.service;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.SourceData;
import com.aspicereporting.exception.SourceFileException;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileParsingService {
    public Source parseCSVFile(MultipartFile file) throws CsvValidationException, IOException {
        CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
        CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream())).withCSVParser(parser).build();
        assert csvReader != null;
        String[] fileRow = csvReader.readNext();

        if (fileRow.length == 0) {
            throw new SourceFileException("Source has no columns defined.");
        }

        //Create new source with columns
        Source source = new Source();
        List<SourceColumn> sourceColumns = new ArrayList<>();
        for (String columnName : fileRow) {
            SourceColumn column = new SourceColumn();
            column.setColumnName(columnName);
            column.setSourceData(new ArrayList<>());
            sourceColumns.add(column);
        }
        while ((fileRow = csvReader.readNext()) != null) {
            for (int i = 0; i < sourceColumns.size(); i++) {
                SourceData data = new SourceData();
                data.setValue(fileRow[i]);
                sourceColumns.get(i).addSourceData(data);
            }
        }
        source.addSourceColumns(sourceColumns);

        return source;
    }
}
