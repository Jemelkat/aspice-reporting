package com.aspicereporting.controller;

import com.aspicereporting.dto.JwtResponseDTO;
import com.aspicereporting.dto.LoginDTO;
import com.aspicereporting.dto.MessageResponseDTO;
import com.aspicereporting.dto.SignupDTO;
import com.aspicereporting.entity.Role;
import com.aspicereporting.entity.User;
import com.aspicereporting.exception.ConstraintException;
import com.aspicereporting.exception.EntityNotFoundException;
import com.aspicereporting.repository.RoleRepository;
import com.aspicereporting.repository.UserRepository;
import com.aspicereporting.security.jwt.JwtUtils;
import com.aspicereporting.security.services.UserDetailsImpl;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@CrossOrigin
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    Validator validator;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginDTO loginDTO) {
        Set<ConstraintViolation<LoginDTO>> result = validator.validate(loginDTO);
        if (!result.isEmpty()) {
            throw new ConstraintException(((ConstraintViolationImpl)result.toArray()[0]).getMessage());
        }

        Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponseDTO(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupDTO signUpDTO) {
        Set<ConstraintViolation<SignupDTO>> result = validator.validate(signUpDTO);
        if (!result.isEmpty()) {
            throw new ConstraintException(((ConstraintViolationImpl)result.toArray()[0]).getMessage());
        }
        if (userRepository.existsByUsername(signUpDTO.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponseDTO("Username is already taken."));
        }

        if (userRepository.existsByEmail(signUpDTO.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponseDTO("Email is already taken."));
        }

        // Create new user's account
        User user = new User(signUpDTO.getUsername(),
                signUpDTO.getEmail(),
                encoder.encode(signUpDTO.getPassword()));

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                .orElseThrow(() -> new EntityNotFoundException("User role not found."));
        roles.add(userRole);

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponseDTO("User registered successfully!"));
    }
}