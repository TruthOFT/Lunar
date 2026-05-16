package com.lunar.lunar_backend.dto;

public record RecordResponse(
        Long id,
        String title,
        String chartName,
        String gender,
        String birthTime,
        String resultJson,
        String createTime
) {
}
