package com.sysforedu.backend.schedule;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ScheduleService {
    private final ScheduleRepository repository;

    public ScheduleService(ScheduleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleItemDto> list(String campus, String course, String teacher,
                                      String room, String weekday, String status) {
        return repository.findAll(campus, course, teacher, room, weekday, status);
    }

    @Transactional
    public ScheduleItemDto create(ScheduleRequests.Create create) {
        repository.lockScheduleWrites();
        validateTimeRange(create.startTime(), create.endTime());
        if (!"已取消".equals(effectiveStatus(create))) {
            ensureNoConflict(create, null, repository.findAllActive());
        }
        return repository.insert(create);
    }

    @Transactional
    public ScheduleItemDto update(String id, ScheduleRequests.Create create) {
        repository.lockScheduleWrites();
        ScheduleItemDto current = requireItem(id);
        validateTimeRange(create.startTime(), create.endTime());
        if (!"已取消".equals(effectiveStatus(create))) {
            ensureNoConflict(create, id, repository.findAllActive());
        }
        repository.update(id, create);
        return repository.findById(current.id());
    }

    @Transactional
    public ScheduleItemDto cancel(String id) {
        repository.lockScheduleWrites();
        ScheduleItemDto current = requireItem(id);
        if (!"已取消".equals(current.status())) {
            repository.cancel(id);
        }
        return repository.findById(id);
    }

    @Transactional
    public List<ScheduleItemDto> createBatch(List<ScheduleRequests.Create> creates) {
        repository.lockScheduleWrites();
        List<ScheduleItemDto> active = new ArrayList<>(repository.findAllActive());
        List<ScheduleItemDto> inserted = new ArrayList<>();
        for (ScheduleRequests.Create create : creates) {
            validateTimeRange(create.startTime(), create.endTime());
            if (!"已取消".equals(effectiveStatus(create))) {
                ensureNoConflict(create, null, active);
            }
            ScheduleItemDto saved = repository.insert(create);
            inserted.add(saved);
            if (!"已取消".equals(saved.status())) {
                active.add(saved);
            }
        }
        return inserted;
    }

    @Transactional(readOnly = true)
    public AutoPlanResponse autoPlan(ScheduleRequests.AutoPlan request) {
        if (request.endTime().isBefore(request.startTime()) || request.endTime().equals(request.startTime())) {
            throw invalid("endTime 必须晚于 startTime");
        }
        if (request.weekdays().size() != new HashSet<>(request.weekdays()).size()) {
            throw invalid("weekdays 不能包含重复的星期");
        }
        if (request.durationMinutes() > java.time.Duration.between(request.startTime(), request.endTime()).toMinutes()) {
            return incomplete(List.of(), request.weeklySessions(), "可选时间范围短于单节课时长，无法生成候选。",
                    "每个候选必须完整落在 " + request.startTime() + " 至 " + request.endTime()
                            + " 的范围内；未找到可用时段。");
        }

        List<ScheduleItemDto> occupied = new ArrayList<>(repository.findAllActive());
        List<ScheduleRequests.Create> candidates = new ArrayList<>();
        Set<String> selectedWeekdays = new HashSet<>();
        List<String> reasons = new ArrayList<>();
        long stepMinutes = 30;
        for (String weekday : request.weekdays()) {
            if (candidates.size() >= request.weeklySessions()) {
                break;
            }
            if (!selectedWeekdays.add(weekday)) {
                continue;
            }
            boolean found = false;
            for (LocalTime start = request.startTime();
                 !start.plusMinutes(request.durationMinutes()).isAfter(request.endTime());
                 start = start.plusMinutes(stepMinutes)) {
                LocalTime end = start.plusMinutes(request.durationMinutes());
                ScheduleRequests.Create proposed = new ScheduleRequests.Create(
                        request.course(), request.className(), request.teacher(), request.room(), request.campus(),
                        weekday, start, end, request.type(), "待上课");
                String conflict = conflictDescription(proposed, null, occupied);
                if (conflict == null) {
                    candidates.add(proposed);
                    occupied.add(asScheduleItem(proposed));
                    found = true;
                    break;
                }
            }
            if (!found) {
                reasons.add(weekday + " 在指定时间范围内没有满足教师、教室和班级资源约束的空档");
            }
        }
        boolean complete = candidates.size() == request.weeklySessions();
        String message = "找到 " + candidates.size() + "/" + request.weeklySessions() + " 节";
        if (!complete && request.weeklySessions() > request.weekdays().size()) {
            reasons.add("指定的 " + request.weekdays().size() + " 个星期少于所需的 "
                    + request.weeklySessions() + " 节");
        }
        String explanation = "按 weekdays 的顺序逐日搜索，每个星期最多安排一节；从 " + request.startTime()
                + " 起每 30 分钟探测一次，单节时长 " + request.durationMinutes()
                + " 分钟。仅返回与现有未取消课程及本方案已选时段不冲突的候选"
                + (reasons.isEmpty() ? "。" : "。未能满足的条件：" + String.join("；", reasons) + "。");
        if (!complete) {
            explanation += " 候选不足，请调整时间范围或资源后重新规划；items 仅供查看，不应保存。";
        }
        return new AutoPlanResponse(List.copyOf(candidates), complete, message, explanation);
    }

    private AutoPlanResponse incomplete(List<ScheduleRequests.Create> items, int required,
                                        String explanation, String reason) {
        return new AutoPlanResponse(items, false, "找到 " + items.size() + "/" + required + " 节",
                explanation + " " + reason + " items 仅供查看，不应保存。");
    }

    private ScheduleItemDto requireItem(String id) {
        ScheduleItemDto item = repository.findById(id);
        if (item == null) {
            throw new ScheduleException(HttpStatus.NOT_FOUND, "未找到排课记录: " + id);
        }
        return item;
    }

    private void ensureNoConflict(ScheduleRequests.Create candidate, String excludedId,
                                  List<ScheduleItemDto> activeItems) {
        String description = conflictDescription(candidate, excludedId, activeItems);
        if (description != null) {
            throw new ScheduleException(HttpStatus.CONFLICT, description);
        }
    }

    private String conflictDescription(ScheduleRequests.Create candidate, String excludedId,
                                       List<ScheduleItemDto> activeItems) {
        for (ScheduleItemDto existing : activeItems) {
            if ((excludedId != null && existing.id() != null && existing.id().equals(excludedId))
                    || "已取消".equals(existing.status())
                    || !existing.weekday().equals(candidate.weekday())) {
                continue;
            }
            boolean overlaps = existing.startTime().isBefore(candidate.endTime())
                    && candidate.startTime().isBefore(existing.endTime());
            if (!overlaps) {
                continue;
            }
            List<String> reasons = new ArrayList<>();
            if (existing.teacher().equals(candidate.teacher())) reasons.add("教师 " + candidate.teacher());
            if (existing.room().equals(candidate.room())) reasons.add("教室 " + candidate.room());
            if (existing.className().equals(candidate.className())) reasons.add("班级 " + candidate.className());
            if (!reasons.isEmpty()) {
                return "排课冲突：" + candidate.weekday() + " " + candidate.startTime() + "-" + candidate.endTime()
                        + " 与原排课 " + existing.id() + "（" + existing.course() + "，" + existing.className()
                        + "，" + existing.teacher() + "，" + existing.room() + "，" + existing.startTime() + "-"
                        + existing.endTime() + "）时间重叠，冲突资源：" + String.join("、", reasons);
            }
        }
        return null;
    }

    private static ScheduleItemDto asScheduleItem(ScheduleRequests.Create create) {
        return new ScheduleItemDto(null, create.course(), create.className(), create.teacher(), create.room(),
                create.campus(), create.weekday(), create.startTime(), create.endTime(), create.type(),
                effectiveStatus(create));
    }

    private static String effectiveStatus(ScheduleRequests.Create create) {
        return create.status() == null || create.status().isBlank() ? "待上课" : create.status();
    }

    private static void validateTimeRange(LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) {
            throw invalid("endTime 必须晚于 startTime");
        }
    }

    private static ScheduleException invalid(String message) {
        return new ScheduleException(HttpStatus.BAD_REQUEST, message);
    }
}
