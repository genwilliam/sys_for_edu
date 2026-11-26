package com.sysforedu.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class AuthRequests {
    private AuthRequests() {
    }

    public record Login(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(max = 128) String password) {
    }

    public record Register(
            @NotBlank @Pattern(regexp = "TEACHER|STUDENT") String role,
            @NotBlank @Size(max = 100) String personId,
            @NotBlank @Pattern(regexp = "[0-9]{11}") String phone,
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_.-]{3,80}") String username,
            @NotBlank @Size(min = 8, max = 128) String password) {
    }

    public record ReviewAccount(@NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String status) {
    }

    public record CreateLeave(
            @NotBlank @Pattern(regexp = "STUDENT_LEAVE|TEACHER_LEAVE") String type,
            String classId,
            LocalDate leaveDate,
            @NotBlank @Size(max = 1000) String reason) {
    }

    public record ReviewLeave(
            @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String status,
            @Size(max = 500) String note) {
    }
}
