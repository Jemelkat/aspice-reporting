package com.aspicereporting.controller;

import com.aspicereporting.dto.MessageResponseDTO;
import com.aspicereporting.dto.SourceTableDTO;
import com.aspicereporting.entity.*;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.service.SourceService;
import com.aspicereporting.utils.NaturalOrderComparator;
import com.fasterxml.jackson.annotation.JsonView;
import net.sf.jasperreports.engine.JRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/source")
public class SourceController {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private SourceService sourceService;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") @Valid @NotBlank(message = "Source file is empty.") MultipartFile file, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();

        //Store multipart file as source
        sourceService.storeFileAsSource(file, loggedUser);

        return ResponseEntity.ok(new MessageResponseDTO(file.getOriginalFilename() + "saved."));
    }

    @GetMapping("/getAll")
    public List<SourceTableDTO> getAll(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        List<Source> sourcesList = sourceService.getAllByUserOrShared(loggedUser);

        //Convert Entity to custom DTO
        return sourcesList
                .stream()
                .map((s) -> {
                    SourceTableDTO sDTO = modelMapper.map(s, SourceTableDTO.class);
                    if (!s.getSourceGroups().isEmpty()) {
                        sDTO.setShared(Boolean.TRUE);
                        sDTO.setSharedBy(s.getUser().getId() == loggedUser.getId() ? "You" : s.getUser().getUsername());
                    }
                    return sDTO;
                })
                .collect(Collectors.toList());
    }

    @JsonView(View.Simple.class)
    @GetMapping("/allSimple")
    public List<Source> getAllSimple(Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        List<Source> sources = sourceService.getAllByUserOrShared(loggedUser);
        Collections.sort(sources, (source1, source2) -> {
            NaturalOrderComparator comparator = new NaturalOrderComparator();
            return comparator.compare(source1.getSourceName(), source2.getSourceName());
        });
        return sources;
    }


    @JsonView(View.Simple.class)
    @GetMapping("/{id}/columns")
    public List<SourceColumn> getColumnsForSource(@PathVariable("id") Long sourceId,Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        List<SourceColumn> sourceColumns = sourceService.getColumnsForSource(sourceId,loggedUser);
        Collections.sort(sourceColumns, (sourceColmn1, sourceColumn2) -> {
            NaturalOrderComparator comparator = new NaturalOrderComparator();
            return comparator.compare(sourceColmn1.getColumnName(), sourceColumn2.getColumnName());
        });
        return sourceColumns;
    }

    @JsonView(View.Simple.class)
    @GetMapping("/columns")
    public List<String> getColumnsForSources(@RequestParam("sources") LinkedHashSet<Long> sources, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        //Convert Set to list
        List<String> sourcesList = new ArrayList<>(sourceService.getColumnsForSources(sources,loggedUser));
        Collections.sort(sourcesList,new NaturalOrderComparator());
        return sourcesList;
    }

    @JsonView(View.Simple.class)
    @GetMapping("/values")
    public List<String> getValuesForSourcesAndColumn(@RequestParam("sources") LinkedHashSet<Long> sources,@RequestParam("columnName") String columnName, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        //Convert Set to list
        List<String> valuesList = new ArrayList<>(sourceService.getColumnValuesForSources(sources,columnName,loggedUser));
        Collections.sort(valuesList,new NaturalOrderComparator());
        return valuesList;
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@Param("sourceId") Long sourceId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        sourceService.deleteById(sourceId, loggedUser);
        return ResponseEntity.ok(new MessageResponseDTO("Source id=" + sourceId + " deleted."));
    }

    @JsonView(View.Simple.class)
    @GetMapping("/{id}/groups")
    public Set<UserGroup> getGroups(@PathVariable("id") Long sourceId, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        return sourceService.getGroupsForSource(sourceId, loggedUser);
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<?> share(@PathVariable("id") Long sourceId, @RequestBody List<Long> groupIds, Authentication authentication) {
        User loggedUser = (User) authentication.getPrincipal();
        sourceService.shareWithGroups(sourceId, groupIds, loggedUser);
        return ResponseEntity.ok(new MessageResponseDTO("Source id=" + sourceId + " shared."));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> generate(@PathVariable("id") Long sourceId, Authentication authentication) throws JRException {
        User loggedUser = (User) authentication.getPrincipal();
        ByteArrayOutputStream out = sourceService.generateCSV(sourceId, loggedUser);
        ByteArrayResource resource = new ByteArrayResource(out.toByteArray());

        HttpHeaders headers = new HttpHeaders(); headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + "file" + ".csv");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/{sourceId}/values")
    public List<String> getDistinctColumnValues(@PathVariable("sourceId") Long sourceId, @RequestParam("columnId") Long columnId, Authentication authentication) throws JRException {
        User loggedUser = (User) authentication.getPrincipal();
        List<String> values = sourceService.getDistinctValuesForColumn(sourceId, columnId, loggedUser);
        Collections.sort(values, new NaturalOrderComparator());
        return values;
    }
}
