package com.fabriciosanches.adamodulo3.apigateway.adapter.in.web;

import com.fabriciosanches.adamodulo3.apigateway.adapter.in.web.dto.LoginRequest;
import com.fabriciosanches.adamodulo3.apigateway.adapter.in.web.dto.LoginResponse;
import com.fabriciosanches.adamodulo3.apigateway.application.LoginUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;

    public AuthController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = loginUseCase.login(request.username(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(new LoginResponse("Bearer", "1200"));
    }
}
