package com.auction.common;

/**
 * 认证异常
 */
public class AuthException extends BizException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public AuthException(int code, String message) {
        super(code, message);
    }
}
