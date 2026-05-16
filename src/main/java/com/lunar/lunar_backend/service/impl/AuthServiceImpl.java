package com.lunar.lunar_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lunar.lunar_backend.dto.AuthResponse;
import com.lunar.lunar_backend.dto.LoginRequest;
import com.lunar.lunar_backend.dto.RegisterRequest;
import com.lunar.lunar_backend.dto.UserResponse;
import com.lunar.lunar_backend.entity.LunarUser;
import com.lunar.lunar_backend.common.ErrorCode;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.mapper.LunarUserMapper;
import com.lunar.lunar_backend.service.AuthService;
import com.lunar.lunar_backend.util.JwtUtil;
import com.lunar.lunar_backend.util.Md5Util;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl extends ServiceImpl<LunarUserMapper, LunarUser> implements AuthService {

    private static final String PASSWORD_SALT = "lunar_password_salt_2026";

    @Override
    public AuthResponse register(RegisterRequest request) {
        validateAccountAndPassword(request.account(), request.password());
        LunarUser exists = getByAccount(request.account());
        if (exists != null) {
            throw new ApiException(ErrorCode.ACCOUNT_EXISTS);
        }

        LunarUser user = new LunarUser();
        user.setAccount(request.account().trim());
        user.setPassword(encryptPassword(request.password()));
        user.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname().trim() : request.account().trim());
        user.setTokenVersion(1);
        save(user);

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        validateAccountAndPassword(request.account(), request.password());
        LunarUser user = getByAccount(request.account());
        if (user == null || !encryptPassword(request.password()).equals(user.getPassword())) {
            throw new ApiException(ErrorCode.ACCOUNT_PASSWORD_ERROR);
        }
        return buildAuthResponse(user);
    }

    @Override
    public UserResponse currentUser(Long userId) {
        LunarUser user = getById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.LOGIN_INVALID);
        }
        return toUserResponse(user);
    }

    @Override
    public LunarUser requireValidTokenUser(Long userId, Integer tokenVersion) {
        LunarUser user = getById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.LOGIN_INVALID);
        }
        if (!tokenVersion.equals(user.getTokenVersion())) {
            throw new ApiException(ErrorCode.LOGIN_INVALID);
        }
        return user;
    }

    private LunarUser getByAccount(String account) {
        return getOne(new LambdaQueryWrapper<LunarUser>()
                .eq(LunarUser::getAccount, account.trim())
                .last("limit 1"));
    }

    private AuthResponse buildAuthResponse(LunarUser user) {
        String token = JwtUtil.createToken(user.getId(), user.getAccount(), user.getTokenVersion());
        return new AuthResponse(token, toUserResponse(user));
    }

    private UserResponse toUserResponse(LunarUser user) {
        return new UserResponse(user.getId(), user.getAccount(), user.getNickname());
    }

    private String encryptPassword(String password) {
        return Md5Util.md5(password + PASSWORD_SALT);
    }

    private void validateAccountAndPassword(String account, String password) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new ApiException(ErrorCode.ACCOUNT_PASSWORD_EMPTY);
        }
        if (account.trim().length() < 3 || account.trim().length() > 32) {
            throw new ApiException(ErrorCode.ACCOUNT_LENGTH_ERROR);
        }
        if (password.length() < 3 || password.length() > 64) {
            throw new ApiException(ErrorCode.PASSWORD_LENGTH_ERROR);
        }
    }
}
