package com.sysforedu.backend.schedule;

import java.util.List;

public record AutoPlanResponse(
        List<ScheduleRequests.Create> items,
        boolean complete,
        String message,
        String explanation) {
}
