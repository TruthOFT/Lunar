package com.lunar.lunar_backend.controller;

import com.lunar.lunar_backend.common.ApiResponse;
import com.lunar.lunar_backend.common.AuthContext;
import com.lunar.lunar_backend.common.AuthUser;
import com.lunar.lunar_backend.dto.UserResponse;
import com.lunar.lunar_backend.service.AuthService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private AuthService authService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(HttpServletRequest request) {
        AuthUser authUser = (AuthUser) request.getAttribute(AuthContext.REQUEST_ATTRIBUTE);
        return ApiResponse.success(authService.currentUser(authUser.userId()));
    }
}
