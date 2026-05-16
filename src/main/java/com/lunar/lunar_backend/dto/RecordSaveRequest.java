package com.lunar.lunar_backend.dto;

public record RecordSaveRequest(
        String title,
        String chartName,
        String gender,
        String birthTime,
        String resultJson
) {
}
