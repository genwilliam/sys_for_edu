package com.sysforedu.backend.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysforedu.backend.auth.TestAdminMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:business_data_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class BusinessDataControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BusinessDataRepository repository;

    @Autowired
    private BusinessDataInitializer initializer;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void authenticateAdminRequests() throws Exception {
        mockMvc = TestAdminMockMvc.withAdmin(context, mockMvc, objectMapper);
    }

    @Test
    void importsMockBusinessDataAndSchedulesOnFirstStartup() throws Exception {
        mockMvc.perform(get("/api/data/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(185)))
                .andExpect(jsonPath("$[0].id").exists());
        mockMvc.perform(get("/api/data/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));
        mockMvc.perform(get("/api/data/class-memberships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].studentIds", hasSize(30)))
                .andExpect(jsonPath("$[1].studentIds", hasSize(30)))
                .andExpect(jsonPath("$[2].studentIds", hasSize(30)))
                .andExpect(jsonPath("$[3].studentIds", hasSize(30)))
                .andExpect(jsonPath("$[4].studentIds", hasSize(30)))
                .andExpect(jsonPath("$[5].studentIds", hasSize(30)));
        mockMvc.perform(get("/api/schedule-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(18)));
        org.junit.jupiter.api.Assertions.assertEquals(30, jdbc.queryForObject(
                "SELECT COUNT(*) FROM schedule_item_students WHERE schedule_item_id = 'SC018'",
                Integer.class));
    }

    @Test
    void createsUpdatesAndDeletesRecordsThroughTheApi() throws Exception {
        String id = "test-" + UUID.randomUUID();
        String body = """
                {"id":"%s","name":"API 测试学生","status":"在读"}
                """.formatted(id);

        mockMvc.perform(post("/api/data/students").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("API 测试学生"));

        mockMvc.perform(put("/api/data/students/{id}", id).contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"已更新学生","status":"停课"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("已更新学生"));

        mockMvc.perform(get("/api/data/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].name".formatted(id)).value(hasSize(1)));

        mockMvc.perform(delete("/api/data/students/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(put("/api/data/students/{id}", id).contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingAndDeletingAClassMaintainsItsMembershipRecord() throws Exception {
        String body = """
                {"id":"class-%s","name":"API 测试班级","course":"数学"}
                """.formatted(UUID.randomUUID());
        String response = mockMvc.perform(post("/api/data/classes")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asText();

        mockMvc.perform(get("/api/data/class-memberships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].studentIds".formatted(id)).exists());
        mockMvc.perform(delete("/api/data/classes/{id}", id)).andExpect(status().isNoContent());

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM business_records WHERE domain_name = 'class-memberships' AND record_id = ?",
                Integer.class, id);
        org.junit.jupiter.api.Assertions.assertEquals(0, remaining);
    }

    @Test
    void rejectsUnknownBusinessDomains() throws Exception {
        mockMvc.perform(get("/api/data/unknown")).andExpect(status().isNotFound());
    }

    @Test
    void upgradesExistingSampleDataWithoutDiscardingExistingMembers() throws Exception {
        repository.update("class-memberships", "K001", java.util.Map.of(
                "id", "K001", "studentIds", java.util.List.of("S001")));
        java.util.Map<String, Object> classRecord = new java.util.LinkedHashMap<>(
                repository.findById("classes", "K001").orElseThrow());
        classRecord.put("capacity", 20);
        classRecord.put("studentCount", 16);
        repository.update("classes", "K001", classRecord);
        repository.delete("students", "S011");
        repository.delete("teachers", "T009");
        jdbc.update("DELETE FROM business_seed_migrations WHERE migration_key = 'sample-rosters-30-v2-accounts'");

        initializer.run();

        java.util.Map<String, Object> membership = repository.findById("class-memberships", "K001")
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(30,
                ((java.util.List<?>) membership.get("studentIds")).size());
        org.junit.jupiter.api.Assertions.assertTrue(
                ((java.util.List<?>) membership.get("studentIds")).contains("S001"));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM business_records WHERE domain_name = 'students' AND record_id = 'S011'",
                Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM business_records WHERE domain_name = 'teachers' AND record_id = 'T009'",
                Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(32,
                repository.findById("classes", "K001").orElseThrow().get("capacity"));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM business_seed_migrations WHERE migration_key = 'sample-rosters-30-v2-accounts'",
                Integer.class));
    }

    @Test
    void classMembershipUpdatesCheckConflictsAndSyncClassSchedules() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String classId = "class-" + suffix;
        String conflictingStudentId = "student-conflict-" + suffix;
        String availableStudentId = "student-available-" + suffix;
        String className = "班级-" + suffix;

        mockMvc.perform(post("/api/data/students").contentType(APPLICATION_JSON)
                        .content("""
                                {"id":"%s","name":"冲突学生"}
                                """.formatted(conflictingStudentId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/data/students").contentType(APPLICATION_JSON)
                        .content("""
                                {"id":"%s","name":"正常学生"}
                                """.formatted(availableStudentId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/data/classes").contentType(APPLICATION_JSON)
                        .content("""
                                {"id":"%s","name":"%s","capacity":5}
                                """.formatted(classId, className)))
                .andExpect(status().isCreated());

        String classScheduleId = createSchedule("class-session-" + suffix, className, "teacher-a-" + suffix,
                "room-a-" + suffix, "[]");
        createSchedule("other-session-" + suffix, "其他-" + suffix, "teacher-b-" + suffix,
                "room-b-" + suffix, "[\"%s\"]".formatted(conflictingStudentId));

        mockMvc.perform(put("/api/data/class-memberships/{id}", classId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"studentIds":["%s"]}
                                """.formatted(conflictingStudentId)))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/data/class-memberships/{id}", classId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"studentIds":["%s"]}
                                """.formatted(availableStudentId)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/schedule-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].studentIds[0]"
                        .formatted(classScheduleId)).value(availableStudentId));

        mockMvc.perform(put("/api/data/class-memberships/{id}", classId)
                        .contentType(APPLICATION_JSON).content("""
                                {"studentIds":[]}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/schedule-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].studentIds[0]"
                        .formatted(classScheduleId)).doesNotExist());
    }

    private String createSchedule(String id, String className, String teacher, String room,
                                   String studentIds) throws Exception {
        String response = mockMvc.perform(post("/api/schedule-items").contentType(APPLICATION_JSON)
                        .content("""
                                {"id":"%s","course":"API 课程","className":"%s","teacher":"%s",
                                 "room":"%s","campus":"API 校区","weekday":"周五",
                                 "startTime":"22:00","endTime":"23:00","type":"常规课",
                                 "studentIds":%s}
                                """.formatted(id, className, teacher, room, studentIds)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asText();
    }
}
