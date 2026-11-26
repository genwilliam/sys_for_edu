package com.sysforedu.backend.schedule;

import org.springframework.http.HttpStatus;

public class ScheduleException extends RuntimeException {
    private final HttpStatus status;

    public ScheduleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
