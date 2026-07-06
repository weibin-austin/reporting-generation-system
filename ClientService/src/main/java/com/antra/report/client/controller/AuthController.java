package com.antra.report.client.controller;

import com.antra.report.client.pojo.reponse.ErrorResponse;
import com.antra.report.client.pojo.reponse.LoginResponse;
import com.antra.report.client.pojo.request.LoginRequest;
import com.antra.report.client.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${app.jwt.expiration-ms:3600000}")
    private long expirationMs;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Validated LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        String token = jwtService.generateToken(request.getUsername());
        log.info("Issued JWT for user {}", request.getUsername());
        return ResponseEntity.ok(new LoginResponse(token, expirationMs));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e) {
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password"),
                HttpStatus.UNAUTHORIZED);
    }
}
