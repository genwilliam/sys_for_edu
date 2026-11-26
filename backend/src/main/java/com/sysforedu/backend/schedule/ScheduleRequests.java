package com.sysforedu.backend.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

public final class ScheduleRequests {
    private ScheduleRequests() {
    }

    public record Create(
            @NotBlank String course,
            @NotBlank String className,
            @NotBlank String teacher,
            @NotBlank String room,
            @NotBlank String campus,
            @NotBlank @Pattern(regexp = "周[一二三四五六日]") String weekday,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            @NotBlank @Pattern(regexp = "常规课|试听课|补课") String type,
            @Pattern(regexp = "待上课|已上课|已取消") String status) {
    }

    public record Batch(@NotEmpty @Size(max = 100) List<@NotNull @Valid Create> items) {
    }

    public record AutoPlan(
            @NotBlank String course,
            @NotBlank String className,
            @NotBlank String teacher,
            @NotBlank String room,
            @NotBlank String campus,
            @Positive @Max(7) int weeklySessions,
            @Positive @Max(1440) int durationMinutes,
            @NotEmpty List<@NotBlank @Pattern(regexp = "周[一二三四五六日]") String> weekdays,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            @NotBlank @Pattern(regexp = "常规课|试听课|补课") String type) {
    }
}
