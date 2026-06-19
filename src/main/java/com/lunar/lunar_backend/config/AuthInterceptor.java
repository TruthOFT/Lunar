package com.lunar.lunar_backend.config;

import com.lunar.lunar_backend.annotation.AuthCheck;
import com.lunar.lunar_backend.common.AuthContext;
import com.lunar.lunar_backend.common.AuthUser;
import com.lunar.lunar_backend.common.ErrorCode;
import com.lunar.lunar_backend.entity.LunarUser;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.service.AuthService;
import com.lunar.lunar_backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String ADMIN_ROLE_NAME = "admin";
    private static final int ADMIN_ROLE_VALUE = 1;

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            throw new ApiException(ErrorCode.NOT_LOGIN);
        }
        AuthUser authUser = JwtUtil.parseToken(token);
        LunarUser user = authService.requireValidTokenUser(authUser.userId(), authUser.tokenVersion());
        checkRole(handler, user);
        request.setAttribute(AuthContext.REQUEST_ATTRIBUTE, authUser);
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization)) {
            String value = authorization.trim();
            if (value.startsWith("Bearer ")) {
                return value.substring("Bearer ".length()).trim();
            }
            return value;
        }
        String token = request.getHeader("token");
        return StringUtils.hasText(token) ? token.trim() : "";
    }

    private void checkRole(Object handler, LunarUser user) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        AuthCheck authCheck = handlerMethod.getMethodAnnotation(AuthCheck.class);
        if (authCheck == null || !StringUtils.hasText(authCheck.role())) {
            return;
        }
        String role = authCheck.role().trim();
        if (ADMIN_ROLE_NAME.equals(role) && !Integer.valueOf(ADMIN_ROLE_VALUE).equals(user.getRole())) {
            throw new ApiException(4030, "无权限访问");
        }
        if (!ADMIN_ROLE_NAME.equals(role)) {
            throw new ApiException(4030, "无权限访问");
        }
    }
}
