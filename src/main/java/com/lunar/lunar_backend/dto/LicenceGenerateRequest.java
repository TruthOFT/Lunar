package com.lunar.lunar_backend.dto;

public record LicenceGenerateRequest(
        Integer count,
        String licenceType,
        Integer aiCount,
        String remark
) {
}
