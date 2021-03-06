package com.aspicereporting.service;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.items.*;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.exception.InvalidDataException;
import com.aspicereporting.repository.SourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Validated
public class ItemValidationService {
    @Autowired
    SourceRepository sourceRepository;

    public void validateItemsWithValid(List<@Valid ReportItem> items, boolean allowUndefinedData, User user) {
        for(ReportItem reportItem : items) {
            validateItem(reportItem, allowUndefinedData, user);
        }
    }

    public void validateItemWithValid(@Valid ReportItem reportItem, User user) {
        validateItem(reportItem, false, user);
    }

    public void validateItem(ReportItem reportItem, boolean allowUndefinedData, User user) {
        if (reportItem instanceof TextItem textItem) {
            //No additional validation
        } else if (reportItem instanceof SimpleTable simpleTable) {
            validateSimpleTable(simpleTable, user, allowUndefinedData);
        } else if (reportItem instanceof CapabilityTable capabilityTable) {
            validateCapabilityTable(capabilityTable, user, allowUndefinedData);
        } else if (reportItem instanceof LevelBarGraph levelBarGraph) {
            validateLevelBarGraph(levelBarGraph, user, allowUndefinedData);
        } else if (reportItem instanceof LevelPieGraph levelPieGraph) {
            validateLevelPieGraph(levelPieGraph, user, allowUndefinedData);
        } else {
            throw new InvalidDataException("Report contains unknown item type: " + reportItem.getType());
        }
    }

    private void validateSimpleTable(SimpleTable simpleTable, User user, boolean allowUndefinedData) {
        if (!allowUndefinedData) {
            //Validate - if source and all id of columns are defined
            simpleTable.validate();
            //Validate - columns are defined
            if (simpleTable.getTableColumns().isEmpty()) {
                throw new InvalidDataException("Simple table id = " + simpleTable.getId() +" has no columns defined.");
            }
        }

        Long sourceId = null;
        if (simpleTable.getSource() != null) {
            sourceId = simpleTable.getSource().getId();
        }
        //Validate source defined
        if (sourceId == null) {
            if (allowUndefinedData) {
                //Clear all other columns if source is not defined
                for (var column : simpleTable.getTableColumns()) {
                    column.setSourceColumn(null);
                    column.setId(null);
                }
            } else {
                throw new InvalidDataException("Simple table id = " + simpleTable.getId() + " needs source defined.");
            }
        } else {
            //Validate - user can use this source id
            Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
            if (source == null) {
                throw new EntityNotFoundException("You dont have any source with id=" + sourceId);
            }

            simpleTable.getTableColumns().forEach(tableColumn -> {
                if (tableColumn.getSourceColumn() != null) {
                    //Validate if column exists in source
                    Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> Objects.equals(c.getId(),tableColumn.getSourceColumn().getId())).findFirst();
                    if (columnExists.isEmpty()) {
                        throw new EntityNotFoundException("Invalid source column: " + tableColumn.getSourceColumn().getColumnName() + " for this simple table source");
                    }
                }
                tableColumn.setId(null);
            });
            simpleTable.setSource(source);
        }
    }

    private void validateCapabilityTable(CapabilityTable capabilityTable, User user, boolean allowUndefinedData) {
        if (!allowUndefinedData) {
            //Validate - if source and all id of columns are defined
            capabilityTable.validate();
        }
        Long sourceId = null;
        if (capabilityTable.getSource() != null) {
            sourceId = capabilityTable.getSource().getId();
        }
        //Validate source defined
        if (sourceId == null) {
            if (allowUndefinedData) {
                //Clear all other columns if source is not defined
                capabilityTable.setAssessorColumn(null);
                capabilityTable.setProcessColumn(null);
                capabilityTable.setCriterionColumn(null);
                capabilityTable.setLevelColumn(null);
                capabilityTable.setScoreColumn(null);
                capabilityTable.getAssessorFilter().clear();
            } else {
                throw new InvalidDataException("Capability table needs source defined.");
            }
        } else {
            //Validate - user can use this source id
            Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
            if (source == null) {
                throw new EntityNotFoundException("Source id= " + sourceId + " does not exist");
            }
            Optional<SourceColumn> columnExists = Optional.empty();

            //ASSESSOR VALIDATE
            if (capabilityTable.getAssessorColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> Objects.equals(c.getId() ,capabilityTable.getAssessorColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getAssessorColumn().getId() + " for source id=" + sourceId);
                }
            }
            //PROCESS VALIDATE
            if (capabilityTable.getProcessColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> Objects.equals(c.getId() ,capabilityTable.getProcessColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getProcessColumn().getId() + " for source id=" + sourceId);
                }
            }
            //LEVEL VALIDATE
            if (capabilityTable.getLevelColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> Objects.equals(c.getId(),capabilityTable.getLevelColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getLevelColumn().getId() + " for source id=" + sourceId);
                }
            }
            //CRITERION VALIDATE
            if (capabilityTable.getCriterionColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> Objects.equals(c.getId(),capabilityTable.getCriterionColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getCriterionColumn().getId() + " for source id=" + sourceId);
                }
            }
            //SCORE VALIDATE
            if (capabilityTable.getScoreColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> Objects.equals(c.getId(),capabilityTable.getScoreColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getScoreColumn().getId() + " for source id=" + sourceId);
                }
            }
            capabilityTable.setSource(source);
        }
    }

    private void validateLevelBarGraph(LevelBarGraph levelBarGraph, User user, boolean allowUndefinedData) {
        if (!allowUndefinedData) {
            //Validate - if source and all id of columns are defined
            levelBarGraph.validate();
        }

        if (levelBarGraph.getSources().isEmpty()) {
            if (allowUndefinedData) {
                levelBarGraph.setAssessorColumnName(null);
                levelBarGraph.setProcessColumnName(null);
                levelBarGraph.setAttributeColumnName(null);
                levelBarGraph.setCriterionColumnName(null);
                levelBarGraph.setScoreColumnName(null);
                levelBarGraph.getAssessorFilter().clear();
            } else {
                throw new InvalidDataException("Level bar graph needs sources defined.");
            }
        } else {
            //Validate if user can access the sources
            List<Source> sources = sourceRepository.findByIdInAndUserOrSourceGroupsIn(levelBarGraph.getSources().stream().map(s -> s.getId()).collect(Collectors.toSet()), user, user.getUserGroups());
            if (sources.size() != levelBarGraph.getSources().size()) {
                throw new InvalidDataException("Level bar graph has inaccessible source defined.");
            }
            //We don't have to set sources - it will not keep the same order of IDs
            //levelBarGraph.setSources(sources);
        }
    }

    private void validateLevelPieGraph(LevelPieGraph levelPieGraph, User user, boolean allowUndefinedData) {
        if (!allowUndefinedData) {
            //Validate - if source and all id of columns are defined
            levelPieGraph.validate();
        }

        Long sourceId = null;
        if (levelPieGraph.getSource() != null) {
            sourceId = levelPieGraph.getSource().getId();
        }
        //Validate source defined
        if (sourceId == null) {
            if (allowUndefinedData) {
                //Clear all other columns if source is not defined
                levelPieGraph.setAssessorColumn(null);
                levelPieGraph.setProcessColumn(null);
                levelPieGraph.setAttributeColumn(null);
                levelPieGraph.setCriterionColumn(null);
                levelPieGraph.setScoreColumn(null);
                levelPieGraph.getAssessorFilter().clear();
            } else {
                throw new InvalidDataException("Level pie graph needs source defined.");
            }
        } else {
            //Validate - user can use this source id
            Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
            if (source == null) {
                throw new EntityNotFoundException("Source id= " + sourceId + " does not exist");
            }
            Optional<SourceColumn> columnExists = Optional.empty();
            //ASSESSOR VALIDATE
            if (levelPieGraph.getAssessorColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> Objects.equals(c.getId(),levelPieGraph.getAssessorColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + levelPieGraph.getAssessorColumn().getId() + " for source id=" + sourceId);
                }
            }
            //PROCESS VALIDATE
            if (levelPieGraph.getProcessColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> Objects.equals(c.getId(),levelPieGraph.getProcessColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + levelPieGraph.getProcessColumn().getId() + " for source id=" + sourceId);
                }
            }
            //CRITERION VALIDATE
            if (levelPieGraph.getCriterionColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> Objects.equals(c.getId(),levelPieGraph.getCriterionColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + levelPieGraph.getCriterionColumn().getId() + " for source id=" + sourceId);
                }
            }
            //ATTRIBUTE VALIDATE
            if (levelPieGraph.getAttributeColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> Objects.equals(c.getId(),levelPieGraph.getAttributeColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + levelPieGraph.getAttributeColumn().getId() + " for source id=" + sourceId);
                }
            }
            //SCORE VALIDATE
            if (levelPieGraph.getScoreColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> Objects.equals(c.getId(),levelPieGraph.getScoreColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + levelPieGraph.getScoreColumn().getId() + " for source id=" + sourceId);
                }
            }
            levelPieGraph.setSource(source);
        }
    }
}
