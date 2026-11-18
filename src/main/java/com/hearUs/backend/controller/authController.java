package com.hearUs.backend.controller;

import com.hearUs.backend.dto.loginRequest;
import com.hearUs.backend.dto.signupRequest;
import com.hearUs.backend.entity.user;
import com.hearUs.backend.repository.userRepository;
import com.hearUs.backend.service.userService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class authController {

    private final userService userService;
    private final userRepository userRepository;

    public authController(userService userService, userRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody signupRequest request) {
        user u = userService.registerUser(request);
        return ResponseEntity.ok(Map.of(
                "id", u.getId(),
                "email", u.getEmail(),
                "role", u.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody loginRequest request) {
        return ResponseEntity.ok(userService.loginUser(request));
    }

    // The frontend calls this right after login to learn the role
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        return userRepository.findByEmail(authentication.getName())
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of(
                        "name", u.getName() == null ? "" : u.getName(),
                        "email", u.getEmail(),
                        "role", u.getRole() == null ? "" : u.getRole(),
                        "department", u.getDepartment() == null ? "" : u.getDepartment())))
                .orElse(ResponseEntity.status(404).build());
    }
}