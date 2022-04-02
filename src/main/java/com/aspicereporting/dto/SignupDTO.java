package com.aspicereporting.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class SignupDTO {
    @NotBlank(message = "username cannot be empty")
    @Size(min = 3, max = 20, message = "Username needs to be between 3 and 20 characters long.")
    private String username;

    @NotBlank(message = "Email cannot be empty")
    @Size(max = 60, message = "Email needs to be max 50 characters long.")
    @Email(message = "Invalid email.")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, max = 50, message = "Password needs to be between 6 and 50 characters long.")
    private String password;
}