package com.sysforedu.backend.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;
import java.util.List;

public record ScheduleItemDto(
        String id,
        String course,
        String className,
        String teacher,
        String room,
        String campus,
        String weekday,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        String type,
        String status,
        List<String> studentIds) {
}
