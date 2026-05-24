package com.auction.common;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    private final int code;

    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
