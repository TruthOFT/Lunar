package com.lunar.lunar_backend.config;

import com.lunar.lunar_backend.common.AuthContext;
import com.lunar.lunar_backend.common.AuthUser;
import com.lunar.lunar_backend.common.ErrorCode;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.service.AuthService;
import com.lunar.lunar_backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new ApiException(ErrorCode.NOT_LOGIN);
        }
        AuthUser authUser = JwtUtil.parseToken(authorization.substring("Bearer ".length()));
        authService.requireValidTokenUser(authUser.userId(), authUser.tokenVersion());
        request.setAttribute(AuthContext.REQUEST_ATTRIBUTE, authUser);
        return true;
    }
}
