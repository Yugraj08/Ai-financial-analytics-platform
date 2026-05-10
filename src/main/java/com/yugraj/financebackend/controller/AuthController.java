package com.yugraj.financebackend.controller;

import com.yugraj.financebackend.dto.LoginRequestDTO;
import com.yugraj.financebackend.dto.UserRequestDTO;
import com.yugraj.financebackend.exception.BadRequestException;
import com.yugraj.financebackend.exception.UnauthorizedException;
import com.yugraj.financebackend.model.Role;
import com.yugraj.financebackend.model.Status;
import com.yugraj.financebackend.model.User;
import com.yugraj.financebackend.repository.UserRepository;
import com.yugraj.financebackend.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public Map<String, String> register(@Valid @RequestBody UserRequestDTO request) {

        // Check email exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        // 🔥 Convert DTO → Entity
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setStatus(Status.ACTIVE);

        // Encode password
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Default role
        if (request.getRole() != null) {
            user.setRole(Role.valueOf(request.getRole()));
        } else {
            user.setRole(Role.VIEWER); // default
        }

        userRepository.save(user);

        return Map.of("message", "User registered successfully");
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequestDTO request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email"));

        if (user.getStatus() == Status.INACTIVE) {
            throw new UnauthorizedException("User account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        return Map.of("token", token, "role", user.getRole().name());
    }
}