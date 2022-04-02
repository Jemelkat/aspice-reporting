package com.aspicereporting.service;

import com.aspicereporting.entity.*;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.exception.SourceFileException;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.UnauthorizedAccessException;
import com.aspicereporting.repository.SourceRepository;
import com.aspicereporting.repository.UserGroupRepository;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
            } else if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls")) {
                source = fileParsingService.parseExcelFile(file);
            }
        } catch (CsvValidationException | IOException e) {
            throw new SourceFileException("Cannot read uploaded file.", e);
        }

        source.setSourceName(file.getOriginalFilename());
        source.setUser(user);
        source.setSourceCreated(new Date());

        sourceRepository.save(source);
    }

    @Transactional
    public void deleteById(Long sourceId, User user) {
        Source source = sourceRepository.findFirstById(sourceId);
        if (source == null) {
            throw new EntityNotFoundException("Could not find source with id = " + sourceId);
        }
        if (!source.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("Only the owner of this source can share it.");
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
        if (!source.getUser().getId().equals(loggedUser.getId())) {
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
            source.removeFromItemsOnUnshare(user, groupIds);
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

    public Set<String> getColumnsForSources(Set<Long> sources, User user) {
        List<Source> sourcesList = sourceRepository.findByIdInAndUserOrSourceGroupsIn(sources, user, user.getUserGroups());

        Set<String> columns = new HashSet<>();
        for(Source source : sourcesList) {
            for(SourceColumn column : source.getSourceColumns()) {
                columns.add(column.getColumnName());
            }
        }

        return columns;
    }

    public List<String> getDistinctValuesForColumn(Long sourceId, Long columnId, User user) {
        Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
        if (source == null) {
            throw new EntityNotFoundException("Could not find data for source id=" + sourceId);
        }
        if (!source.getSourceColumns().stream().anyMatch(sourceColumn -> sourceColumn.getId().equals(columnId))) {
            throw new EntityNotFoundException("Source id="+sourceId +" has no column id="+columnId);
        }
        List<String> columValues = sourceRepository.findDistinctColumnValuesForColumn(columnId);
        return columValues.stream().filter(name -> !name.equals("")).collect(Collectors.toList());
    }

    public ByteArrayOutputStream generateCSV(Long sourceId, User user) {
        Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
        if (source == null) {
            throw new EntityNotFoundException("Could not find source with id = " + sourceId);
        }
        return fileParsingService.parseSourceToCSV(source);
    }

    public Set<String> getColumnValuesForSources(LinkedHashSet<Long> sources, String columnName, User user) {
        Set<String> values = new HashSet<>();
        for(Long sourceId : sources) {
            Source existingSource = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
            if(existingSource == null) {
                throw new EntityNotFoundException("Could not find data for source id = " + sourceId);
            }
            SourceColumn sourceColumn = getSourceColumnByName(existingSource, columnName);
            List<String> columValues = sourceRepository.findDistinctColumnValuesForColumn(sourceColumn.getId());
            values.addAll(columValues.stream().filter(name -> !name.equals("")).collect(Collectors.toList()));
        }
        return values;
    }

    private SourceColumn getSourceColumnByName(Source source, String name) {
        for (SourceColumn sourceColumn : source.getSourceColumns()) {
            if (sourceColumn.getColumnName().equals(name)) {
                return sourceColumn;
            }
        }
        throw new InvalidDataException("Source: " + source.getSourceName() + " has no column named: " + name + ". Cannot get assessor filter data.");
    }
}
