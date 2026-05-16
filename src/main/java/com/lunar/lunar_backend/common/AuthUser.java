package com.lunar.lunar_backend.common;

public record AuthUser(Long userId, String account, Integer tokenVersion) {
}
