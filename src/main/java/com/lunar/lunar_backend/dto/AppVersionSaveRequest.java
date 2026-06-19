package com.lunar.lunar_backend.dto;

public record AppVersionSaveRequest(
        Integer versionCode,
        String versionName,
        String downloadUrl,
        String changelog,
        Boolean forceUpdate
) {}
