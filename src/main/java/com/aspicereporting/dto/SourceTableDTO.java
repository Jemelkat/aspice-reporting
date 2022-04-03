package com.aspicereporting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SourceTableDTO {
    private Long id;
    private String sourceName;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date sourceCreated;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date sourceLastUpdated;

    private Boolean shared = Boolean.FALSE;
    private String sharedBy;
}
