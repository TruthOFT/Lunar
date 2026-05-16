package com.lunar.lunar_backend.exception;

import com.lunar.lunar_backend.common.ErrorCode;

public class ApiException extends RuntimeException {

    private final int code;

    private final int httpStatus;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = 400;
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
