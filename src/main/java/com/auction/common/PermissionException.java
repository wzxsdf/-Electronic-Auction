package com.auction.common;

/**
 * 权限异常
 */
public class PermissionException extends BizException {

    public PermissionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PermissionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public PermissionException(int code, String message) {
        super(code, message);
    }
}
