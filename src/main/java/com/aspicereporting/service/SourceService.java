package com.aspicereporting.service;

import com.aspicereporting.entity.*;
import com.aspicereporting.exception.SourceFileException;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.UnauthorizedAccessException;
import com.aspicereporting.repository.SourceRepository;
import com.aspicereporting.repository.UserGroupRepository;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.aspicereporting.utils.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class SourceService {

    @Autowired
    SourceRepository sourceRepository;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    FileParsingService fileParsingService;

    public void storeFileAsSource(MultipartFile file, User user) {

        String fileName = file.getOriginalFilename();
        Source source = null;
        try {
            if (fileName.toLowerCase().endsWith(".csv")) {
                source = fileParsingService.parseCSVFile(file);
            }
        } catch (CsvValidationException | IOException e) {
            throw new SourceFileException("Cannot read uploaded file.", e);
        }

        source.setSourceName(file.getOriginalFilename());
        source.setUser(user);
        source.setSourceCreated(new Date());

        //Parse csv to objects
        //source.setSourceColumns(parseFileToColumnsList(file, source));

        sourceRepository.save(source);
    }

    @Transactional
    public void deleteById(Long sourceId, User user) {
        Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
        if (source == null) {
            throw new EntityNotFoundException("Could not find source with id = " + sourceId);
        }
        //Source was found - delete it
        source.prepareForDelete();
        sourceRepository.delete(source);
    }

    //Get all owned or shared sources
    public List<Source> getAllByUserOrShared(User user) {
        return sourceRepository.findDistinctByUserOrSourceGroupsIn(user, user.getUserGroups());
    }

    public Set<UserGroup> getGroupsForSource(Long sourceId, User loggedUser) {
        Source source = sourceRepository.findFirstById(sourceId);
        if (source == null) {
            throw new EntityNotFoundException("Could not find source with id = " + sourceId);
        }
        if (source.getUser().getId() != loggedUser.getId()) {
            throw new UnauthorizedAccessException("Only the owner of this source can share it.");
        }

        return source.getSourceGroups();
    }

    //Share selected source with selected groups
    public void shareWithGroups(Long sourceId, List<Long> groupIds, User user) {
        Source source = sourceRepository.findByIdAndUser(sourceId, user);
        if (source == null) {
            throw new EntityNotFoundException("Could not find source with id = " + sourceId);
        }

        //Get all groups for update
        List<UserGroup> sourceGroupList = userGroupRepository.findAllByIdIn(groupIds);

        //Get all removed groups
        Set<UserGroup> removedGroups = new HashSet<>(source.getSourceGroups());
        removedGroups.removeAll(sourceGroupList);

        //Remove removed groups
        for (UserGroup group : removedGroups) {
            source.removeGroup(group);
        }
        //Add new groups
        for (UserGroup group : sourceGroupList) {
            source.addGroup(group);
        }
        sourceRepository.save(source);
    }

    public List<SourceColumn> getColumnsForSource(Long sourceId, User user) {
        Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
        if (source == null) {
            throw new EntityNotFoundException("Could not find data for source id=" + sourceId);
        }
        return source.getSourceColumns();
    }

    public ByteArrayOutputStream generateCSV(Long sourceId, User user) {
        Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
        if (source == null) {
            throw new EntityNotFoundException("Could not find source with id = " + sourceId);
        }
        return fileParsingService.parseSourceToCSV(source);
    }
}
