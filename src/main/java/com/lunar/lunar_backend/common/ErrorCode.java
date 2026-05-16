package com.lunar.lunar_backend.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    PARAMS_ERROR(4000, "请求参数错误", 400),
    ACCOUNT_EXISTS(4001, "账号已存在", 400),
    ACCOUNT_PASSWORD_ERROR(4002, "账号或密码错误", 400),
    ACCOUNT_PASSWORD_EMPTY(4003, "账号和密码不能为空", 400),
    ACCOUNT_LENGTH_ERROR(4004, "账号长度需为 4 到 32 位", 400),
    PASSWORD_LENGTH_ERROR(4005, "密码长度需为 6 到 24 位", 400),
    CHART_RESULT_EMPTY(4006, "排盘结果不能为空", 400),
    NOT_LOGIN(4007, "请先登录", 401),
    LOGIN_INVALID(4008, "登录已失效", 401),
    LOGIN_EXPIRED(4009, "登录已过期", 401),
    AI_CONFIG_MISSING(4010, "AI配置缺失", 500),
    AI_ANALYZE_FAILED(4011, "AI分析失败", 502);

    private final int code;

    private final String message;

    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
