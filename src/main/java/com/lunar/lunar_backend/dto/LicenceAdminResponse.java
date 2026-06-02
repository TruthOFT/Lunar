package com.lunar.lunar_backend.dto;

public record LicenceAdminResponse(
        Long id,
        String licenceCode,
        String licenceType,
        Integer durationDays,
        String expireTime,
        Integer status,
        String remark,
        Long userId,
        String usedTime,
        String createTime
) {
}
