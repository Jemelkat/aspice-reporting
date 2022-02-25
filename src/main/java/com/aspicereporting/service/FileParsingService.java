package com.aspicereporting.service;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.SourceData;
import com.aspicereporting.exception.SourceFileException;
import com.aspicereporting.utils.CsvFileUtils;
import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class FileParsingService {
    public Source parseCSVFile(MultipartFile file) throws CsvValidationException, IOException {
        CSVParser parser = new CSVParserBuilder().withSeparator(CsvFileUtils.detectDelimiter(file.getInputStream())).build();
        CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream())).withCSVParser(parser).build();
        assert csvReader != null;
        String[] fileRow = csvReader.readNext();

        if (fileRow == null) {
            throw new SourceFileException("Source file has no data.");
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

        csvReader.close();
        return source;
    }

    public Source parseExcelFile(MultipartFile file) throws IOException {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        Source source = new Source();

        Iterator<Row> fileIterator = sheet.iterator();
        if(!fileIterator.hasNext()) {
            throw new SourceFileException("Source file has no data.");
        }
        //Create columns from first row - header rows
        List<SourceColumn> sourceColumns = new ArrayList<>();
        Row headerRow = fileIterator.next();
        for(Cell cell : headerRow) {
            SourceColumn sourceColumn = new SourceColumn();
            sourceColumn.setColumnName(cell.getStringCellValue());
            sourceColumn.setSource(source);
            sourceColumns.add(sourceColumn);
        }

        //Add data to each row
        while (fileIterator.hasNext()) {
            Row row = fileIterator.next();
            for(int i=0; i<sourceColumns.size(); i++) {
                SourceData data = new SourceData();
                data.setValue(row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue());
                data.setSourceColumn(sourceColumns.get(i));
                sourceColumns.get(i).addSourceData(data);
            }
        }

        source.setSourceColumns(sourceColumns);
        return source;
    }

    public ByteArrayOutputStream parseSourceToCSV(Source source) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream), ',',
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);


            int numOfRows = 0;
            //Find column with max rows
            for (var sourceColumn : source.getSourceColumns()) {
                int tempRows = sourceColumn.getSourceData().size();
                if (tempRows > numOfRows) {
                    numOfRows = tempRows;
                }
            }

            //Write column names
            List<String> row = new ArrayList<>();
            for (var column : source.getSourceColumns()) {
                row.add(column.getColumnName());
            }
            if (!row.isEmpty()) {
                csvWriter.writeNext(row.toArray(new String[0]));
            }

            //Combine to all columns to line and write to output stream
            for (int i = 0; i < numOfRows; i++) {
                row.clear();
                for (var column : source.getSourceColumns()) {
                    if (column.getSourceData().size() > numOfRows) {
                        row.add("");
                    } else {
                        row.add(column.getSourceData().get(i).getValue());
                    }
                }
                csvWriter.writeNext(row.toArray(new String[0]));
            }

            csvWriter.close();
        } catch (IOException e) {
            throw new SourceFileException("Error creating CSV from source", e);
        }
        return outputStream;
    }
}
