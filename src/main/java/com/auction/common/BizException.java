package com.auction.common;

public class BizException extends BaseException {

    public BizException(int code, String message) {
        super(code, message);
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BizException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
