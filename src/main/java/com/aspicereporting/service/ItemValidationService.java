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

import java.util.Optional;

@Service
public class ItemValidationService {
    @Autowired
    SourceRepository sourceRepository;

    public void validateItem(ReportItem reportItem, boolean allowUndefinedData, User user) {
        if (reportItem instanceof TextItem textItem) {
            //No validation
        } else if (reportItem instanceof TableItem tableItem) {
            validateSimpleTable(tableItem, user);
        } else if (reportItem instanceof CapabilityTable capabilityTable) {
            validateCapabilityTable(capabilityTable, user);
        } else if (reportItem instanceof CapabilityBarGraph capabilityBarGraph) {
            if(!allowUndefinedData) {
                validateCapabilityBarGraph(capabilityBarGraph, user);
            }
        } else {
            throw new InvalidDataException("Report contains unknown item type: " + reportItem.getType());
        }
    }

    private void validateSimpleTable(TableItem tableItem, User user) {
        //Validate - columns are defined
        if (tableItem.getTableColumns().isEmpty()) {
            throw new InvalidDataException("Simple table has no columns defined.");
        }
        //Validate - source is defined
        if (tableItem.getSource().getId() == null) {
            throw new InvalidDataException("Simple table has no source defined.");
        }

        Long sourceId = tableItem.getSource().getId();
        //Validate - user can use this source id
        Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
        if (source == null) {
            throw new EntityNotFoundException("You dont have any source with id=" + sourceId);
        }

        //Table columns are reinserted every time - not updated
        tableItem.getTableColumns().forEach(tableColumn -> {
            //Validate if column exists in source
            Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(tableColumn.getSourceColumn().getId())).findFirst();
            if (columnExists.isEmpty()) {
                throw new EntityNotFoundException("Invalid source column id=" + tableColumn.getSourceColumn().getId() + " for source id=" + sourceId);
            }
            tableColumn.setId(null);
        });
        source.addSimpleTable(tableItem);
    }

    private void validateCapabilityTable(CapabilityTable capabilityTable, User user) {
        //Validate - columns are defined
        if (capabilityTable.getProcessColumn() == null || capabilityTable.getProcessColumn().getSourceColumn() == null) {
            throw new InvalidDataException("capability table has no process column defined.");
        }
        //Validate - source is defined
        if (capabilityTable.getSource().getId() == null) {
            throw new InvalidDataException("Simple table has no source defined.");
        }
        Long sourceId = capabilityTable.getSource().getId();
        //Validate - user can use this source id
        Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
        if (source == null) {
            throw new EntityNotFoundException("You dont have any source with id=" + sourceId);
        }

        //PROCESS VALIDATE
        Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityTable.getProcessColumn().getSourceColumn().getId())).findFirst();
        if (columnExists.isEmpty()) {
            throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getProcessColumn().getSourceColumn().getId() + " for source id=" + sourceId);
        }
        //LEVEL VALIDATE
        columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityTable.getLevelColumn().getId())).findFirst();
        if (columnExists.isEmpty()) {
            throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getLevelColumn().getId() + " for source id=" + sourceId);
        }
        //ENGINEERING VALIDATE
        columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityTable.getCriterionColumn().getId())).findFirst();
        if (columnExists.isEmpty()) {
            throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getCriterionColumn().getId() + " for source id=" + sourceId);
        }
        //SCORE VALIDATE
        columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityTable.getScoreColumn().getId())).findFirst();
        if (columnExists.isEmpty()) {
            throw new EntityNotFoundException("Invalid source column id=" + capabilityTable.getScoreColumn().getId() + " for source id=" + sourceId);
        }
        capabilityTable.getProcessColumn().setId(null);
    }

    private void validateCapabilityBarGraph(CapabilityBarGraph capabilityBarGraph, User user) {
        //Validate - if source and all id of columns are defined
        capabilityBarGraph.validate();

        Long sourceId = capabilityBarGraph.getSource().getId();
        //Validate - user can use this source id
        Source source = sourceRepository.findByIdAndUserOrSourceGroupsIn(sourceId, user, user.getUserGroups());
        if (source == null) {
            throw new EntityNotFoundException("Source id= " + sourceId +" does not exist");
        }
        //PROCESS VALIDATE
        Optional<SourceColumn> columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityBarGraph.getProcessColumn().getId())).findFirst();
        if (columnExists.isEmpty()) {
            throw new EntityNotFoundException("Invalid source column id=" + capabilityBarGraph.getProcessColumn().getId() + " for source id=" + sourceId);
        }
        //LEVEL VALIDATE
        columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityBarGraph.getLevelColumn().getId())).findFirst();
        if (columnExists.isEmpty()) {
            throw new EntityNotFoundException("Invalid source column id=" + capabilityBarGraph.getLevelColumn().getId() + " for source id=" + sourceId);
        }
        //ATTRIBUTE VALIDATE
        columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityBarGraph.getAttributeColumn().getId())).findFirst();
        if (columnExists.isEmpty()) {
            throw new EntityNotFoundException("Invalid source column id=" + capabilityBarGraph.getAttributeColumn().getId() + " for source id=" + sourceId);
        }
        //SCORE VALIDATE
        columnExists = source.getSourceColumns().stream().filter((c) -> c.getId().equals(capabilityBarGraph.getScoreColumn().getId())).findFirst();
        if (columnExists.isEmpty()) {
            throw new EntityNotFoundException("Invalid source column id=" + capabilityBarGraph.getScoreColumn().getId() + " for source id=" + sourceId);
        }
        source.addCapabilityGraph(capabilityBarGraph);
    }
}
