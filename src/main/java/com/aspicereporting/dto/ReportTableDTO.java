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
public class ReportTableDTO {
    private Long reportId;
    private String reportName;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date reportCreated;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date reportLastUpdated;

    private Boolean shared = Boolean.FALSE;
    private String sharedBy;
}
