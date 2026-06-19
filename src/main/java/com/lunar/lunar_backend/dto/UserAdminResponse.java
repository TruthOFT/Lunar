package com.lunar.lunar_backend.dto;

public record UserAdminResponse(
        Long id,
        String account,
        String nickname,
        Integer role,
        String createTime
) {}
