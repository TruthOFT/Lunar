package com.lunar.lunar_backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lunar.lunar_backend.annotation.AuthCheck;
import com.lunar.lunar_backend.common.ApiResponse;
import com.lunar.lunar_backend.common.AuthContext;
import com.lunar.lunar_backend.common.AuthUser;
import com.lunar.lunar_backend.dto.UserAdminResponse;
import com.lunar.lunar_backend.dto.UserResponse;
import com.lunar.lunar_backend.dto.UserUpdateRoleRequest;
import com.lunar.lunar_backend.service.AuthService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/list")
    @AuthCheck(role = "admin")
    public ApiResponse<IPage<UserAdminResponse>> list(
            @RequestParam Integer page,
            @RequestParam Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(authService.listUsers(page, pageSize, keyword));
    }

    @PutMapping("/{id}/role")
    @AuthCheck(role = "admin")
    public ApiResponse<Void> updateRole(
            @PathVariable Long id,
            @RequestBody UserUpdateRoleRequest requestBody) {
        authService.updateUserRole(id, requestBody.role());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @AuthCheck(role = "admin")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        authService.deleteUser(id);
        return ApiResponse.success(null);
    }
}
