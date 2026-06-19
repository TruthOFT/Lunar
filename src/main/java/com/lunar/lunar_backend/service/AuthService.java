package com.lunar.lunar_backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lunar.lunar_backend.dto.AuthResponse;
import com.lunar.lunar_backend.dto.LoginRequest;
import com.lunar.lunar_backend.dto.RegisterRequest;
import com.lunar.lunar_backend.dto.UserAdminResponse;
import com.lunar.lunar_backend.dto.UserResponse;
import com.lunar.lunar_backend.entity.LunarUser;

public interface AuthService extends IService<LunarUser> {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse currentUser(Long userId);

    LunarUser requireValidTokenUser(Long userId, Integer tokenVersion);

    IPage<UserAdminResponse> listUsers(Integer page, Integer pageSize, String keyword);

    void updateUserRole(Long id, Integer role);

    void deleteUser(Long id);
}
