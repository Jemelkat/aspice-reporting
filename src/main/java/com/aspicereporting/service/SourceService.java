package com.aspicereporting.service;

import com.aspicereporting.entity.*;
import com.aspicereporting.exception.CsvSourceFileException;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.repository.SourceRepository;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.aspicereporting.utils.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SourceService {

    @Autowired
    SourceRepository sourceRepository;

    public void storeFileAsSource(MultipartFile file, User user) {
        Source source = new Source();
        source.setSourceName(file.getOriginalFilename());
        source.setUser(user);
        source.setSourceCreated(new Date());
        source.setSourceLastUpdated(new Date());

        //Parse csv to objects
        source.setSourceColumns(parseFileToColumnsList(file, source));

        sourceRepository.save(source);
    }

    @Transactional
    public void deleteSourceById(Long sourceId, User user) {
        Source source = sourceRepository.findBySourceIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
        if(source == null) {
            throw new EntityNotFoundException("Could not find source with id = " + sourceId);
        }
        //Source was found - delete it
        source.removeFromAllGroups();
        sourceRepository.delete(source);
    }

    public List<Source> getSourcesByUser(User user) {
        //Get all owned or shared sources
        return sourceRepository.findDistinctByUserOrSourceGroupsIn(user, user.getUserGroups());
    }

    private List<SourceColumn> parseFileToColumnsList(MultipartFile file, Source source) {
        List<SourceColumn> sourceColumns = new ArrayList<>();
        Character delimiter = null;
        try {
            delimiter = CsvFileUtils.detectDelimiter(file.getInputStream());

            CSVParser parser = new CSVParserBuilder().withSeparator(delimiter).build();
            CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream())).withCSVParser(parser).build();
            String[] rowData = null;

            //Read data header
            if ((rowData = csvReader.readNext()) != null) {
                for (var data : rowData) {
                    SourceColumn sourceColumn = new SourceColumn(data);
                    sourceColumn.setSource(source);
                    sourceColumns.add(sourceColumn);
                }
            }
            else {
                throw new CsvSourceFileException("Loaded file does not contain header.");
            }

            //Read data values for headers
            while ((rowData = csvReader.readNext()) != null) {
                for (int i = 0; i < sourceColumns.size(); i++) {
                    sourceColumns.get(i).addSourceData(new SourceData(rowData[i]));
                }
            }

        } catch (IOException | CsvValidationException e) {
            throw new CsvSourceFileException("There was error processing CSV file.", e);
        }
        return sourceColumns;
    }
}
