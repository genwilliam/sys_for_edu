package com.sysforedu.backend.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class BusinessDataRepository {
    private static final TypeReference<LinkedHashMap<String, Object>> RECORD_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public BusinessDataRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> findAll(String domain) {
        return jdbc.query("SELECT payload FROM business_records WHERE domain_name = ? ORDER BY record_id",
                (rs, rowNum) -> decode(rs.getString("payload")), domain);
    }

    public Optional<Map<String, Object>> findById(String domain, String id) {
        return jdbc.query("SELECT payload FROM business_records WHERE domain_name = ? AND record_id = ?",
                        (rs, rowNum) -> decode(rs.getString("payload")), domain, id)
                .stream().findFirst();
    }

    public void insert(String domain, Map<String, Object> record) {
        try {
            jdbc.update("INSERT INTO business_records (domain_name, record_id, payload) VALUES (?, ?, ?)",
                    domain, record.get("id"), encode(record));
        } catch (DuplicateKeyException exception) {
            throw exception;
        }
    }

    public boolean update(String domain, String id, Map<String, Object> record) {
        return jdbc.update("""
                UPDATE business_records SET payload = ?, updated_at = CURRENT_TIMESTAMP
                WHERE domain_name = ? AND record_id = ?
                """, encode(record), domain, id) > 0;
    }

    public boolean delete(String domain, String id) {
        return jdbc.update("DELETE FROM business_records WHERE domain_name = ? AND record_id = ?",
                domain, id) > 0;
    }

    public int count(String domain) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM business_records WHERE domain_name = ?", Integer.class, domain);
        return count == null ? 0 : count;
    }

    public void insertSeed(String domain, List<Map<String, Object>> records) {
        for (Map<String, Object> record : records) {
            Object id = record.get("id");
            if (id instanceof String recordId && !recordId.isBlank()) {
                jdbc.update("INSERT INTO business_records (domain_name, record_id, payload) VALUES (?, ?, ?)",
                        domain, recordId, encode(record));
            }
        }
    }

    public void insertSeedMissing(String domain, List<Map<String, Object>> records) {
        for (Map<String, Object> record : records) {
            Object id = record.get("id");
            if (id instanceof String recordId && !recordId.isBlank()
                    && findById(domain, recordId).isEmpty()) {
                insert(domain, record);
            }
        }
    }

    public void removeStudentFromMemberships(String studentId) {
        for (Map<String, Object> membership : findAll("class-memberships")) {
            Object rawStudentIds = membership.get("studentIds");
            if (!(rawStudentIds instanceof List<?> studentIds) || !studentIds.contains(studentId)) continue;
            Map<String, Object> updated = new LinkedHashMap<>(membership);
            updated.put("studentIds", studentIds.stream().filter(id -> !studentId.equals(id)).toList());
            update("class-memberships", String.valueOf(membership.get("id")), updated);
        }
        jdbc.update("DELETE FROM schedule_item_students WHERE student_id = ?", studentId);
    }

    public boolean hasOverlappingSchedule(String className, String studentId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM schedule_items candidate
                JOIN schedule_items existing
                  ON existing.weekday = candidate.weekday
                 AND existing.start_time < candidate.end_time
                 AND candidate.start_time < existing.end_time
                 AND existing.id <> candidate.id
                 AND existing.status <> '已取消'
                JOIN schedule_item_students enrolled
                  ON enrolled.schedule_item_id = existing.id
                 AND enrolled.student_id = ?
                WHERE candidate.class_name = ?
                  AND candidate.status <> '已取消'
                """, Integer.class, studentId, className);
        return count != null && count > 0;
    }

    public void addStudentToClassSchedules(String className, String studentId) {
        jdbc.update("""
                INSERT INTO schedule_item_students (schedule_item_id, student_id)
                SELECT id, ? FROM schedule_items
                WHERE class_name = ? AND status <> '已取消'
                AND NOT EXISTS (
                    SELECT 1 FROM schedule_item_students
                    WHERE schedule_item_id = schedule_items.id AND student_id = ?
                )
                """, studentId, className, studentId);
    }

    public void removeStudentFromClassSchedules(String className, String studentId) {
        jdbc.update("""
                DELETE FROM schedule_item_students
                WHERE student_id = ? AND schedule_item_id IN (
                    SELECT id FROM schedule_items WHERE class_name = ?
                )
                """, studentId, className);
    }

    public void backfillClassScheduleStudents(Map<String, List<String>> studentsByClass) {
        List<Map<String, String>> unassignedItems = jdbc.query("""
                SELECT item.id, item.class_name
                FROM schedule_items item
                WHERE NOT EXISTS (
                    SELECT 1 FROM schedule_item_students assigned
                    WHERE assigned.schedule_item_id = item.id
                )
                """, (rs, rowNum) -> Map.of("id", rs.getString("id"), "className", rs.getString("class_name")));
        for (Map<String, String> item : unassignedItems) {
            for (String studentId : studentsByClass.getOrDefault(item.get("className"), List.of())) {
                jdbc.update("INSERT INTO schedule_item_students (schedule_item_id, student_id) VALUES (?, ?)",
                        item.get("id"), studentId);
            }
        }
    }

    private String encode(Map<String, Object> record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("业务记录无法序列化", exception);
        }
    }

    private Map<String, Object> decode(String payload) {
        try {
            return objectMapper.readValue(payload, RECORD_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("数据库中的业务记录格式无效", exception);
        }
    }
}
