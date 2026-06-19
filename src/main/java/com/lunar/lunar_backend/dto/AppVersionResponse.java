package com.lunar.lunar_backend.dto;

public record AppVersionResponse(
        Integer versionCode,
        String versionName,
        String downloadUrl,
        String changelog,
        Boolean forceUpdate
) {}
