package com.sysforedu.backend.business;

import com.sysforedu.backend.auth.AuthContext;
import com.sysforedu.backend.auth.AuthPrincipal;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sysforedu.backend.schedule.ScheduleRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class BusinessDataService {
    private static final Set<String> DOMAINS = Set.of(
            "campuses", "teachers", "students", "classes", "courses", "classrooms",
            "attendance", "orders", "visits", "users", "roles", "class-memberships");
    private static final Map<String, List<String>> REQUIRED_FIELDS = Map.ofEntries(
            Map.entry("campuses", List.of("name")),
            Map.entry("teachers", List.of("name", "subject")),
            Map.entry("students", List.of("name")),
            Map.entry("classes", List.of("name")),
            Map.entry("courses", List.of("name")),
            Map.entry("classrooms", List.of("name")),
            Map.entry("attendance", List.of("student")),
            Map.entry("orders", List.of("student", "course")),
            Map.entry("visits", List.of("student", "content")),
            Map.entry("users", List.of("username", "name")),
            Map.entry("roles", List.of("name", "code")));
    private static final int MAX_ID_LENGTH = 100;

    private final BusinessDataRepository repository;
    private final ScheduleRepository scheduleRepository;

    public BusinessDataService(BusinessDataRepository repository, ScheduleRepository scheduleRepository) {
        this.repository = repository;
        this.scheduleRepository = scheduleRepository;
    }

    public List<Map<String, Object>> list(String domain) {
        requireDomain(domain);
        List<Map<String, Object>> records = repository.findAll(domain);
        AuthPrincipal principal = AuthContext.current();
        if (principal == null || principal.isAdmin()) return records;

        List<Map<String, Object>> classes = repository.findAll("classes");
        Set<String> classIds = new LinkedHashSet<>();
        Set<String> classNames = new LinkedHashSet<>();
        Set<String> courseNames = new LinkedHashSet<>();
        Set<String> roomNames = new LinkedHashSet<>();
        Set<String> teacherNames = new LinkedHashSet<>();
        Set<String> relatedStudentIds = new LinkedHashSet<>();
        if ("TEACHER".equals(principal.role())) {
            classes.stream().filter(record -> principal.name().equals(record.get("teacher")))
                    .forEach(record -> collectClass(record, classIds, classNames, courseNames, roomNames,
                            teacherNames));
            for (Map<String, Object> membership : repository.findAll("class-memberships")) {
                if (classIds.contains(String.valueOf(membership.get("id")))) {
                    relatedStudentIds.addAll(studentIds(membership));
                }
            }
        } else if ("STUDENT".equals(principal.role())) {
            relatedStudentIds.add(principal.personId());
            for (Map<String, Object> membership : repository.findAll("class-memberships")) {
                if (studentIds(membership).contains(principal.personId())) {
                    String classId = String.valueOf(membership.get("id"));
                    classIds.add(classId);
                    classes.stream().filter(record -> classId.equals(record.get("id")))
                            .forEach(record -> collectClass(record, classIds, classNames, courseNames, roomNames,
                                    teacherNames));
                }
            }
        }

        return records.stream().filter(record -> visibleTo(principal, domain, record, classIds,
                        classNames, courseNames, roomNames, teacherNames, relatedStudentIds))
                .map(record -> "STUDENT".equals(principal.role()) && "class-memberships".equals(domain)
                        ? Map.<String, Object>of("id", record.get("id"),
                        "studentIds", List.of(principal.personId()))
                        : record).toList();
    }

    @Transactional
    public Map<String, Object> create(String domain, Map<String, Object> input) {
        requireDomain(domain);
        Map<String, Object> record = copy(input);
        Object rawId = record.get("id");
        String id = rawId instanceof String value && !value.isBlank() ? value.trim() : UUID.randomUUID().toString();
        validateId(id);
        record.put("id", id);
        validateRequiredFields(domain, record);
        validateMembership(domain, id, record);
        String membershipClassName = null;
        if ("class-memberships".equals(domain)) {
            scheduleRepository.lockScheduleWrites();
            membershipClassName = className(id);
            ensureNoStudentScheduleConflict(membershipClassName, studentIds(record));
        }
        try {
            repository.insert(domain, record);
        } catch (DuplicateKeyException exception) {
            throw new BusinessDataException(HttpStatus.CONFLICT, "该业务记录 ID 已存在");
        }
        if (membershipClassName != null) {
            for (String studentId : studentIds(record)) {
                repository.addStudentToClassSchedules(membershipClassName, studentId);
            }
        }
        if ("classes".equals(domain)) {
            repository.insert("class-memberships", Map.of("id", id, "studentIds", List.of()));
        }
        return record;
    }

    @Transactional
    public Map<String, Object> update(String domain, String id, Map<String, Object> input) {
        requireDomain(domain);
        validateId(id);
        Map<String, Object> record = copy(input);
        record.put("id", id);
        validateRequiredFields(domain, record);
        validateMembership(domain, id, record);
        List<String> addedStudents = List.of();
        List<String> removedStudents = List.of();
        String className = "";
        if ("class-memberships".equals(domain)) {
            scheduleRepository.lockScheduleWrites();
            List<String> previousIds = studentIds(repository.findById(domain, id).orElse(Map.of()));
            List<String> nextIds = studentIds(record);
            addedStudents = nextIds.stream().filter(studentId -> !previousIds.contains(studentId)).toList();
            removedStudents = previousIds.stream().filter(studentId -> !nextIds.contains(studentId)).toList();
            className = className(id);
            ensureNoStudentScheduleConflict(className, addedStudents);
        }
        if (!repository.update(domain, id, record)) {
            throw new BusinessDataException(HttpStatus.NOT_FOUND, "未找到要更新的业务记录");
        }
        for (String studentId : removedStudents) {
            repository.removeStudentFromClassSchedules(className, studentId);
        }
        for (String studentId : addedStudents) {
            repository.addStudentToClassSchedules(className, studentId);
        }
        return record;
    }

    @Transactional
    public void delete(String domain, String id) {
        requireDomain(domain);
        validateId(id);
        Map<String, Object> membership = Map.of();
        String membershipClassName = null;
        if ("class-memberships".equals(domain)) {
            scheduleRepository.lockScheduleWrites();
            membership = repository.findById(domain, id)
                    .orElseThrow(() -> new BusinessDataException(HttpStatus.NOT_FOUND, "班级成员记录不存在"));
            membershipClassName = className(id);
        }
        if (!repository.delete(domain, id)) {
            throw new BusinessDataException(HttpStatus.NOT_FOUND, "未找到要删除的业务记录");
        }
        if ("classes".equals(domain)) repository.delete("class-memberships", id);
        if ("students".equals(domain)) repository.removeStudentFromMemberships(id);
        if ("class-memberships".equals(domain)) {
            for (String studentId : studentIds(membership)) {
                repository.removeStudentFromClassSchedules(membershipClassName, studentId);
            }
        }
    }

    private static Map<String, Object> copy(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            throw new BusinessDataException(HttpStatus.BAD_REQUEST, "业务记录不能为空");
        }
        return new LinkedHashMap<>(input);
    }

    private static void validateId(String id) {
        if (id == null || id.isBlank() || id.length() > MAX_ID_LENGTH) {
            throw new BusinessDataException(HttpStatus.BAD_REQUEST, "业务记录 ID 无效");
        }
    }

    private void validateMembership(String domain, String classId, Map<String, Object> record) {
        if (!"class-memberships".equals(domain)) return;
        if (repository.findById("classes", classId).isEmpty()) {
            throw new BusinessDataException(HttpStatus.NOT_FOUND, "班级不存在");
        }
        Object rawStudentIds = record.get("studentIds");
        if (!(rawStudentIds instanceof List<?> studentIds)) {
            throw new BusinessDataException(HttpStatus.BAD_REQUEST, "studentIds 必须是数组");
        }
        LinkedHashSet<String> uniqueStudentIds = new LinkedHashSet<>();
        for (Object studentId : studentIds) {
            if (!(studentId instanceof String id) || id.isBlank()) {
                throw new BusinessDataException(HttpStatus.BAD_REQUEST, "学生 ID 无效");
            }
            String normalizedId = id.trim();
            if (repository.findById("students", normalizedId).isEmpty()) {
                throw new BusinessDataException(HttpStatus.NOT_FOUND, "学生不存在：" + normalizedId);
            }
            uniqueStudentIds.add(normalizedId);
        }
        record.put("studentIds", List.copyOf(uniqueStudentIds));
        Object rawCapacity = repository.findById("classes", classId).orElseThrow().get("capacity");
        int capacity = rawCapacity instanceof Number number ? number.intValue() : Integer.MAX_VALUE;
        if (uniqueStudentIds.size() > capacity) {
            throw new BusinessDataException(HttpStatus.CONFLICT, "班级人数已超过容量");
        }
    }

    private static void validateRequiredFields(String domain, Map<String, Object> record) {
        for (String field : REQUIRED_FIELDS.getOrDefault(domain, List.of())) {
            Object value = record.get(field);
            if (!(value instanceof String text) || text.isBlank()) {
                throw new BusinessDataException(HttpStatus.BAD_REQUEST, field + " 不能为空");
            }
        }
    }

    private static List<String> studentIds(Map<String, Object> record) {
        Object value = record.get("studentIds");
        if (!(value instanceof List<?> ids)) return List.of();
        return ids.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private static void collectClass(Map<String, Object> record, Set<String> classIds,
                                     Set<String> classNames, Set<String> courseNames, Set<String> roomNames,
                                     Set<String> teacherNames) {
        Object id = record.get("id");
        Object name = record.get("name");
        Object course = record.get("course");
        Object room = record.get("room");
        Object teacher = record.get("teacher");
        if (id instanceof String value) classIds.add(value);
        if (name instanceof String value) classNames.add(value);
        if (course instanceof String value) courseNames.add(value);
        if (room instanceof String value) roomNames.add(value);
        if (teacher instanceof String value) teacherNames.add(value);
    }

    private static boolean visibleTo(AuthPrincipal principal, String domain, Map<String, Object> record,
                                     Set<String> classIds, Set<String> classNames, Set<String> courseNames,
                                     Set<String> roomNames, Set<String> teacherNames,
                                     Set<String> relatedStudentIds) {
        String personId = principal.personId();
        String personName = principal.name();
        return switch (domain) {
            case "campuses" -> String.valueOf(record.get("name")).contains(principal.campus());
            case "teachers" -> "TEACHER".equals(principal.role())
                    ? personId.equals(record.get("id"))
                    : teacherNames.contains(String.valueOf(record.get("name")));
            case "students" -> "STUDENT".equals(principal.role())
                    ? personId.equals(record.get("id"))
                    : relatedStudentIds.contains(String.valueOf(record.get("id")));
            case "classes" -> classIds.contains(String.valueOf(record.get("id")));
            case "class-memberships" -> {
                if (!classIds.contains(String.valueOf(record.get("id")))) yield false;
                yield true;
            }
            case "courses" -> "TEACHER".equals(principal.role())
                    ? personName.equals(record.get("teacher")) || courseNames.contains(record.get("name"))
                    : courseNames.contains(record.get("name"));
            case "classrooms" -> roomNames.contains(record.get("name"));
            case "attendance" -> "TEACHER".equals(principal.role())
                    ? personName.equals(record.get("teacher")) || classNames.contains(record.get("className"))
                    : personName.equals(record.get("student"));
            case "orders", "visits" -> "STUDENT".equals(principal.role())
                    && personName.equals(record.get("student"));
            default -> false;
        };
    }

    private String className(String classId) {
        return String.valueOf(repository.findById("classes", classId).orElseThrow().get("name"));
    }

    private void ensureNoStudentScheduleConflict(String className, List<String> studentIds) {
        for (String studentId : studentIds) {
            if (repository.hasOverlappingSchedule(className, studentId)) {
                throw new BusinessDataException(HttpStatus.CONFLICT,
                        "学生 " + studentId + " 与班级现有课表存在时间冲突");
            }
        }
    }

    private static void requireDomain(String domain) {
        if (!DOMAINS.contains(domain)) {
            throw new BusinessDataException(HttpStatus.NOT_FOUND, "不支持的业务数据类型");
        }
    }
}
