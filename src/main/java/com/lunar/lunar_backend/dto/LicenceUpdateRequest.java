package com.lunar.lunar_backend.dto;

public record LicenceUpdateRequest(
        String licenceType,
        Integer durationDays,
        String remark
) {
}
