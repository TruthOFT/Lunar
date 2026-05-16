package com.lunar.lunar_backend.controller;

import com.lunar.lunar_backend.common.ApiResponse;
import com.lunar.lunar_backend.dto.AuthResponse;
import com.lunar.lunar_backend.dto.LoginRequest;
import com.lunar.lunar_backend.dto.RegisterRequest;
import com.lunar.lunar_backend.service.AuthService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
