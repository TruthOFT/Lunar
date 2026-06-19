package com.lunar.lunar_backend.dto;

public record LicenceResponse(
        Long id,
        String licenceCode,
        String licenceType,
        String expireTime,
        Integer status,
        String remark,
        String createTime
) {
}
