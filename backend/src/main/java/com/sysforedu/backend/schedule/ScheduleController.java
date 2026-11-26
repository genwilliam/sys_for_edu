package com.sysforedu.backend.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedule-items")
@Validated
public class ScheduleController {
    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public List<ScheduleItemDto> list(
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String teacher,
            @RequestParam(required = false) String room,
            @RequestParam(required = false) @Pattern(regexp = "周[一二三四五六日]") String weekday,
            @RequestParam(required = false) @Pattern(regexp = "待上课|已上课|已取消") String status) {
        return service.list(campus, course, teacher, room, weekday, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleItemDto create(@Valid @RequestBody ScheduleRequests.Create create) {
        return service.create(create);
    }

    @PutMapping("/{id}")
    public ScheduleItemDto update(@PathVariable String id, @Valid @RequestBody ScheduleRequests.Create create) {
        return service.update(id, create);
    }

    @PatchMapping("/{id}/cancel")
    public ScheduleItemDto cancel(@PathVariable String id) {
        return service.cancel(id);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ScheduleItemDto> createBatch(@Valid @RequestBody ScheduleRequests.Batch batch) {
        return service.createBatch(batch.items());
    }

    @PostMapping("/auto-plan")
    public AutoPlanResponse autoPlan(@Valid @RequestBody ScheduleRequests.AutoPlan request) {
        return service.autoPlan(request);
    }
}
