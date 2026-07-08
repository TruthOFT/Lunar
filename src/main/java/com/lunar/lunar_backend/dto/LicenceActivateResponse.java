package com.lunar.lunar_backend.dto;

public record LicenceActivateResponse(Boolean isVip, String vipExpireTime, String licenceType, Integer aiRemainCount) {
}
