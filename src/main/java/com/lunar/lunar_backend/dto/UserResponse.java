package com.lunar.lunar_backend.dto;

public record UserResponse(
        Long id,
        String account,
        String nickname,
        Boolean isVip,
        String vipExpireTime,
        String licenceType
) {
}
