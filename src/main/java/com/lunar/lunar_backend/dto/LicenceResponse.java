package com.lunar.lunar_backend.dto;

public record LicenceResponse(
        Long id,
        String licenceCode,
        String licenceType,
        Integer durationDays,
        String expireTime,
        Integer status,
        String remark,
        String createTime
) {
}
