package org.bootcamp.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bootcamp.authservice.dto.LoginRequest;
import org.bootcamp.authservice.dto.LoginResponse;
import org.bootcamp.authservice.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request) {

        if (!"admin".equals(request.getUsername())
            || !"123456".equals(request.getPassword())) {

            return ResponseEntity.status(401).build();
        }

        String token = jwtService.generateToken(request.getUsername());

        return ResponseEntity.ok(
            LoginResponse.builder()
                .accessToken(token)
                .build()
        );
    }
}
