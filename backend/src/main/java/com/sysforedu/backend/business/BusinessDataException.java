package com.sysforedu.backend.business;

import org.springframework.http.HttpStatus;

public class BusinessDataException extends RuntimeException {
    private final HttpStatus status;

    public BusinessDataException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
