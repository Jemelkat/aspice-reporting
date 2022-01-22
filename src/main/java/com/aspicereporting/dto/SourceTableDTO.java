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
    private Long sourceId;
    private String sourceName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sourceCreated;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sourceLastUpdated;

    private Boolean shared = Boolean.FALSE;
    private String sharedBy;
}
