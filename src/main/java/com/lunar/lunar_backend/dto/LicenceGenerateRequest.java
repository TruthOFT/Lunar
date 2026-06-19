package com.lunar.lunar_backend.dto;

public record LicenceGenerateRequest(
        Integer count,
        String licenceType,
        String remark
) {
}
