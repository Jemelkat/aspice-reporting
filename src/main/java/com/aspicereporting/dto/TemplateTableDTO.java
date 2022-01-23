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
public class TemplateTableDTO {
    private Long id;
    private String templateName;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date templateCreated;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date templateLastUpdated;

    private Boolean shared = Boolean.FALSE;
    private String sharedBy;
}
