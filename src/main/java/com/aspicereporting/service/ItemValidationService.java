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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Validated
public class ItemValidationService {
    @Autowired
    SourceRepository sourceRepository;

    public void validateItemWithValid(@Valid ReportItem reportItem, boolean allowUndefinedData, User user) {
        validateItem(reportItem, allowUndefinedData, user);
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
                throw new InvalidDataException("Simple table has no columns defined.");
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
                }
            } else {
                throw new InvalidDataException("Simple table needs source defined.");
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
                    Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(tableColumn.getSourceColumn().getId())).findFirst();
                    if (columnExists.isEmpty()) {
                        throw new EntityNotFoundException("Invalid source column: " + tableColumn.getSourceColumn().getColumnName() + " for this simple table source");
                    }
                } else {
                    if (!allowUndefinedData) {
                        throw new InvalidDataException("Simple table needs all columns defined.");
                    }
                }
                tableColumn.setId(null);
            });
            source.addSimpleTable(simpleTable);
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
                capabilityTable.setProcessColumn(null);
                capabilityTable.setCriterionColumn(null);
                capabilityTable.setLevelColumn(null);
                capabilityTable.setScoreColumn(null);
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

            //PROCESS VALIDATE
            if (capabilityTable.getProcessColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityTable.getProcessColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getProcessColumn().getId() + " for source id=" + sourceId);
                }
            } else {
                if (!allowUndefinedData) {
                    throw new InvalidDataException("Capability table needs process column defined.");
                }
            }
            //LEVEL VALIDATE
            if (capabilityTable.getLevelColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityTable.getLevelColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getLevelColumn().getId() + " for source id=" + sourceId);
                }
            } else {
                if (!allowUndefinedData) {
                    throw new InvalidDataException("Capability table needs level column defined.");
                }
            }
            //CRITERION VALIDATE
            if (capabilityTable.getCriterionColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityTable.getCriterionColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getCriterionColumn().getId() + " for source id=" + sourceId);
                }
            } else {
                if (!allowUndefinedData) {
                    throw new InvalidDataException("Capability table needs criterion column defined.");
                }
            }
            //SCORE VALIDATE
            if (capabilityTable.getScoreColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityTable.getScoreColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getScoreColumn().getId() + " for source id=" + sourceId);
                }
            } else {
                if (!allowUndefinedData) {
                    throw new InvalidDataException("Capability table needs score column defined.");
                }
            }
            source.addCapabilityTable(capabilityTable);
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
                levelBarGraph.setScoreColumnName(null);
            } else {
                throw new InvalidDataException("Level bar graph needs sources defined.");
            }
        } else {
            //Validate if user can access the sources
            List<Source> sources = sourceRepository.findByIdInAndUserOrSourceGroupsIn(levelBarGraph.getSources().stream().map(s -> s.getId()).collect(Collectors.toSet()), user, user.getUserGroups());
            if (sources.size() != levelBarGraph.getSources().size()) {
                throw new InvalidDataException("Level bar graph has inaccessible source defined.");
            }

            for (Source source : sources) {
                source.getLevelBarGraphs().add(levelBarGraph);
            }
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
                levelPieGraph.setProcessColumn(null);
                levelPieGraph.setAttributeColumn(null);
                levelPieGraph.setCriterionColumn(null);
                levelPieGraph.setScoreColumn(null);
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
            //PROCESS VALIDATE
            if (levelPieGraph.getProcessColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(levelPieGraph.getProcessColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + levelPieGraph.getProcessColumn().getId() + " for source id=" + sourceId);
                }
            } else {
                if (!allowUndefinedData) {
                    throw new InvalidDataException("Level pie graph needs process column defined.");
                }
            }
            //LEVEL VALIDATE
            if (levelPieGraph.getCriterionColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(levelPieGraph.getCriterionColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + levelPieGraph.getCriterionColumn().getId() + " for source id=" + sourceId);
                }
            } else {
                if (!allowUndefinedData) {
                    throw new InvalidDataException("Level pie graph needs level column defined.");
                }
            }
            //ATTRIBUTE VALIDATE
            if (levelPieGraph.getAttributeColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(levelPieGraph.getAttributeColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + levelPieGraph.getAttributeColumn().getId() + " for source id=" + sourceId);
                }
            } else {
                if (!allowUndefinedData) {
                    throw new InvalidDataException("Level pie graph needs attribute column defined.");
                }
            }
            //SCORE VALIDATE
            if (levelPieGraph.getScoreColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(levelPieGraph.getScoreColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + levelPieGraph.getScoreColumn().getId() + " for source id=" + sourceId);
                }
            } else {
                if (!allowUndefinedData) {
                    throw new InvalidDataException("Level pie graph needs score column defined.");
                }
            }
            source.addLevelPieGraph(levelPieGraph);
        }
    }
}
