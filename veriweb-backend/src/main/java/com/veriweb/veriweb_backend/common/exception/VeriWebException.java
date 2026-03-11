package com.veriweb.veriweb_backend.common.exception;

import lombok.Getter;

@Getter
public class VeriWebException extends RuntimeException {

    private final ErrorCode errorCode;

    public VeriWebException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
