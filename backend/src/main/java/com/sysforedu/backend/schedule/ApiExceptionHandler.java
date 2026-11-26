package com.sysforedu.backend.schedule;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ScheduleException.class)
    public org.springframework.http.ResponseEntity<ApiError> handleSchedule(ScheduleException exception) {
        return response(exception.status(), exception.status().getReasonPhrase(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException exception) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::formatFieldError)
                .distinct()
                .collect(Collectors.joining("; "));
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                details.isBlank() ? "请求参数无效" : "请求参数无效：" + details);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraint(Exception exception) {
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "Bad Request", "请求参数无效：" + exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadable(HttpMessageNotReadableException exception) {
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "请求 JSON 无效或字段格式错误");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnexpected(Exception exception) {
        logger.error("Unhandled API error", exception);
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", "服务器内部错误");
    }

    private static String formatFieldError(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    private static org.springframework.http.ResponseEntity<ApiError> response(
            HttpStatus status, String error, String message) {
        return org.springframework.http.ResponseEntity.status(status)
                .body(new ApiError(status.value(), error, message));
    }

    public record ApiError(int status, String error, String message) {
    }
}
