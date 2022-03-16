package com.aspicereporting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDTO {
    private String token;
    private final String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
}
