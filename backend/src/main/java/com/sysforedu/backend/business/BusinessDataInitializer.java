package com.sysforedu.backend.business;

import com.sysforedu.backend.auth.AuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
public class BusinessDataInitializer implements org.springframework.boot.CommandLineRunner {
    private static final String ROSTER_MIGRATION_KEY = "sample-rosters-30-v2-accounts";
    private static final TypeReference<LinkedHashMap<String, Object>> SEED_TYPE = new TypeReference<>() {};
    private static final List<String> DOMAINS = List.of(
            "campuses", "teachers", "students", "classes", "courses", "classrooms",
            "attendance", "orders", "visits", "users", "roles", "class-memberships");

    private final BusinessDataRepository repository;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AuthService authService;

    public BusinessDataInitializer(BusinessDataRepository repository, JdbcTemplate jdbc, ObjectMapper objectMapper,
                                   AuthService authService) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.authService = authService;
    }

    @Override
    public void run(String... args) throws IOException {
        Map<String, Object> seed = objectMapper.readValue(
                new ClassPathResource("db/seed/business-data.json").getInputStream(), SEED_TYPE);
        Object rawCollections = seed.get("collections");
        if (!(rawCollections instanceof Map<?, ?> collections)) {
            throw new IllegalStateException("初始业务数据缺少 collections");
        }
        for (String domain : DOMAINS) {
            if (repository.count(domain) == 0) {
                repository.insertSeed(domain, records(collections.get(domain)));
            }
        }
        applyRosterUpgrade(collections);
        seedScheduleItems(seed);
        authService.ensureDemoAccounts();
    }

    private void applyRosterUpgrade(Map<?, ?> collections) {
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM business_seed_migrations WHERE migration_key = ?",
                Integer.class, ROSTER_MIGRATION_KEY);
        if (applied != null && applied > 0) return;

        repository.insertSeedMissing("teachers", records(collections.get("teachers")));
        repository.insertSeedMissing("students", records(collections.get("students")));
        refreshMaskedSamplePhones("teachers", collections.get("teachers"));
        refreshMaskedSamplePhones("students", collections.get("students"));

        Map<String, Map<String, Object>> classRecords = new LinkedHashMap<>();
        for (Map<String, Object> classRecord : repository.findAll("classes")) {
            Object id = classRecord.get("id");
            if (id instanceof String classId) classRecords.put(classId, classRecord);
        }
        Map<String, Map<String, Object>> seedClasses = new LinkedHashMap<>();
        for (Map<String, Object> seedClass : records(collections.get("classes"))) {
            Object id = seedClass.get("id");
            if (id instanceof String classId) seedClasses.put(classId, seedClass);
        }
        for (Map<String, Object> seedMembership : records(collections.get("class-memberships"))) {
            Object rawId = seedMembership.get("id");
            if (!(rawId instanceof String classId)) continue;
            LinkedHashSet<String> mergedIds = new LinkedHashSet<>();
            Object rawPreviousIds = repository.findById("class-memberships", classId)
                    .map(record -> record.get("studentIds")).orElse(null);
            if (rawPreviousIds instanceof List<?> previousIds) {
                previousIds.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .forEach(mergedIds::add);
            }
            Object rawSeedIds = seedMembership.get("studentIds");
            if (rawSeedIds instanceof List<?> seedIds) {
                seedIds.stream().filter(String.class::isInstance)
                        .map(String.class::cast).forEach(mergedIds::add);
            }
            Map<String, Object> membership = Map.of("id", classId, "studentIds", List.copyOf(mergedIds));
            if (repository.findById("class-memberships", classId).isPresent()) {
                repository.update("class-memberships", classId, membership);
            } else {
                repository.insert("class-memberships", membership);
            }

            Map<String, Object> classRecord = classRecords.get(classId);
            if (classRecord == null) continue;
            Map<String, Object> updatedClass = new LinkedHashMap<>(classRecord);
            updatedClass.put("capacity", Math.max(number(classRecord.get("capacity")), 32));
            updatedClass.put("studentCount",
                    Math.max(number(classRecord.get("studentCount")), mergedIds.size()));
            Map<String, Object> seedClass = seedClasses.getOrDefault(classId, Map.of());
            if ("未开班".equals(classRecord.get("status")) && "开课中".equals(seedClass.get("status"))) {
                updatedClass.put("status", seedClass.get("status"));
            }
            if ("待排课".equals(classRecord.get("schedule")) && seedClass.get("schedule") instanceof String) {
                updatedClass.put("schedule", seedClass.get("schedule"));
            }
            repository.update("classes", classId, updatedClass);
        }
        jdbc.update("INSERT INTO business_seed_migrations (migration_key) VALUES (?)", ROSTER_MIGRATION_KEY);
    }

    private void refreshMaskedSamplePhones(String domain, Object rawSeedRecords) {
        for (Map<String, Object> seedRecord : records(rawSeedRecords)) {
            Object rawId = seedRecord.get("id");
            Object seedPhone = seedRecord.get("phone");
            if (!(rawId instanceof String id) || !(seedPhone instanceof String phone)
                    || !phone.matches("[0-9]{11}")) continue;
            repository.findById(domain, id).ifPresent(existing -> {
                Object existingPhone = existing.get("phone");
                if (existingPhone instanceof String value && value.contains("*")) {
                    Map<String, Object> updated = new LinkedHashMap<>(existing);
                    updated.put("phone", phone);
                    repository.update(domain, id, updated);
                }
            });
        }
    }

    private void seedScheduleItems(Map<String, Object> seed) {
        Object rawSchedules = seed.get("schedules");

        Map<String, List<String>> studentsByClass = new LinkedHashMap<>();
        Map<String, String> classNamesById = new LinkedHashMap<>();
        for (Map<String, Object> classRecord : repository.findAll("classes")) {
            if (classRecord.get("id") instanceof String id && classRecord.get("name") instanceof String name) {
                classNamesById.put(id, name);
            }
        }
        for (Map<String, Object> membership : repository.findAll("class-memberships")) {
            Object classId = membership.get("id");
            Object rawStudentIds = membership.get("studentIds");
            if (!(classId instanceof String id) || !(rawStudentIds instanceof List<?> studentIds)) continue;
            String className = classNamesById.get(id);
            if (className != null) {
                studentsByClass.put(className, studentIds.stream()
                        .filter(String.class::isInstance).map(String.class::cast).toList());
            }
        }

        if (!(rawSchedules instanceof List<?> schedules)) return;

        for (Object value : schedules) {
            if (!(value instanceof Map<?, ?> item)) continue;
            String id = text(item, "id");
            String className = text(item, "className");
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM schedule_items WHERE id = ?", Integer.class, id);
            if (count != null && count > 0) continue;
            jdbc.update("""
                    INSERT INTO schedule_items
                    (id, course, class_name, teacher, room, campus, weekday, start_time, end_time, type, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, text(item, "course"), className, text(item, "teacher"), text(item, "room"),
                    text(item, "campus"), text(item, "weekday"), Time.valueOf(text(item, "startTime") + ":00"),
                    Time.valueOf(text(item, "endTime") + ":00"), text(item, "type"), text(item, "status"));
        }
        for (Map.Entry<String, List<String>> classStudents : studentsByClass.entrySet()) {
            for (String studentId : classStudents.getValue()) {
                repository.addStudentToClassSchedules(classStudents.getKey(), studentId);
            }
        }
        repository.backfillClassScheduleStudents(studentsByClass);
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static List<Map<String, Object>> records(Object value) {
        if (!(value instanceof List<?> values)) return List.of();
        List<Map<String, Object>> records = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> record = new LinkedHashMap<>();
                map.forEach((key, itemValue) -> {
                    if (key instanceof String stringKey) record.put(stringKey, itemValue);
                });
                records.add(record);
            }
        }
        return records;
    }

    private static String text(Map<?, ?> item, String key) {
        Object value = item.get(key);
        return value instanceof String text ? text : "";
    }
}
