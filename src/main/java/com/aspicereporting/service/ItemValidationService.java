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
import java.util.Optional;

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
        } else if (reportItem instanceof TableItem tableItem) {
            validateSimpleTable(tableItem, user, allowUndefinedData);
        } else if (reportItem instanceof CapabilityTable capabilityTable) {
            validateCapabilityTable(capabilityTable, user, allowUndefinedData);
        } else if (reportItem instanceof CapabilityBarGraph capabilityBarGraph) {
            validateCapabilityBarGraph(capabilityBarGraph, user, allowUndefinedData);
        } else if (reportItem instanceof LevelPieGraph levelPieGraph) {
            validateLevelPieGraph(levelPieGraph, user, allowUndefinedData);
        } else {
            throw new InvalidDataException("Report contains unknown item type: " + reportItem.getType());
        }
    }

    private void validateSimpleTable(TableItem tableItem, User user, boolean allowUndefinedData) {
        if (!allowUndefinedData) {
            //Validate - if source and all id of columns are defined
            tableItem.validate();
            //Validate - columns are defined
            if (tableItem.getTableColumns().isEmpty()) {
                throw new InvalidDataException("Simple table has no columns defined.");
            }
        }

        Long sourceId = null;
        if (tableItem.getSource() != null) {
            sourceId = tableItem.getSource().getId();
        }
        //Validate source defined
        if (sourceId == null) {
            if (allowUndefinedData) {
                //Clear all other columns if source is not defined
                for (var column : tableItem.getTableColumns()) {
                    column.setSourceColumn(null);
                }
            } else {
                throw new InvalidDataException("Capability table needs source defined.");
            }
        } else {
            //Validate - user can use this source id
            Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
            if (source == null) {
                throw new EntityNotFoundException("You dont have any source with id=" + sourceId);
            }

            tableItem.getTableColumns().forEach(tableColumn -> {
                if (tableColumn.getSourceColumn() != null) {
                    //Validate if column exists in source
                    Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(tableColumn.getSourceColumn().getId())).findFirst();
                    if (columnExists.isEmpty()) {
                        throw new EntityNotFoundException("Invalid source column: " + tableColumn.getSourceColumn().getColumnName() + " for this simple table source");
                    }
                } else {
                    if (!allowUndefinedData) {
                        throw  new InvalidDataException("Simple table needs all columns defined.");
                    }
                }
                tableColumn.setId(null);
            });
            source.addSimpleTable(tableItem);
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
                    throw new InvalidDataException("Capability bar graph needs level column defined.");
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
                    throw new InvalidDataException("Capability bar graph needs level column defined.");
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
                    throw new InvalidDataException("Capability bar graph needs level column defined.");
                }
            }
            source.addCapabilityTable(capabilityTable);
        }
    }

    private void validateCapabilityBarGraph(CapabilityBarGraph capabilityBarGraph, User user, boolean allowUndefinedData) {
        if (!allowUndefinedData) {
            //Validate - if source and all id of columns are defined
            capabilityBarGraph.validate();
        }
        Long sourceId = null;
        if (capabilityBarGraph.getSource() != null) {
            sourceId = capabilityBarGraph.getSource().getId();
        }
        //Validate source defined
        if (sourceId == null) {
            if (allowUndefinedData) {
                //Clear all other columns if source is not defined
                capabilityBarGraph.setProcessColumn(null);
                capabilityBarGraph.setAttributeColumn(null);
                capabilityBarGraph.setLevelColumn(null);
                capabilityBarGraph.setScoreColumn(null);
                capabilityBarGraph.setAssessorColumn(null);
                capabilityBarGraph.getProcessFilter().clear();
                capabilityBarGraph.getAssessorFilter().clear();
            } else {
                throw new InvalidDataException("Capability bar graph needs source defined.");
            }
        } else {
            //Validate - user can use this source id
            Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
            if (source == null) {
                throw new EntityNotFoundException("Source id= " + sourceId + " does not exist");
            }
            Optional<SourceColumn> columnExists = Optional.empty();
            //PROCESS VALIDATE
            if (capabilityBarGraph.getProcessColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityBarGraph.getProcessColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityBarGraph.getProcessColumn().getId() + " for source id=" + sourceId);
                }
            } else {
                if (!allowUndefinedData) {
                    throw new InvalidDataException("Capability bar graph needs process column defined.");
                }
            }
            //LEVEL VALIDATE
            if (capabilityBarGraph.getLevelColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityBarGraph.getLevelColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityBarGraph.getLevelColumn().getId() + " for source id=" + sourceId);
                }
            } else {
                if (!allowUndefinedData) {
                    throw new InvalidDataException("Capability bar graph needs level column defined.");
                }
            }
            //ATTRIBUTE VALIDATE
            if (capabilityBarGraph.getAttributeColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityBarGraph.getAttributeColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityBarGraph.getAttributeColumn().getId() + " for source id=" + sourceId);
                }
            } else {
                if (!allowUndefinedData) {
                    throw new InvalidDataException("Capability bar graph needs attribute column defined.");
                }
            }
            //SCORE VALIDATE
            if (capabilityBarGraph.getScoreColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityBarGraph.getScoreColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + capabilityBarGraph.getScoreColumn().getId() + " for source id=" + sourceId);
                }
            } else {
                if (!allowUndefinedData) {
                    throw new InvalidDataException("Capability bar graph needs score column defined.");
                }
            }
            source.addCapabilityGraph(capabilityBarGraph);
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
                levelPieGraph.setLevelColumn(null);
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
            if (levelPieGraph.getLevelColumn() != null) {
                columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(levelPieGraph.getLevelColumn().getId())).findFirst();
                if (columnExists.isEmpty()) {
                    throw new EntityNotFoundException("Invalid source column id=" + levelPieGraph.getLevelColumn().getId() + " for source id=" + sourceId);
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
