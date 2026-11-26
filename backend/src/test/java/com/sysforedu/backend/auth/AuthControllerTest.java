package com.sysforedu.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:business_data_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc adminMvc;
    private String adminToken;

    @BeforeEach
    void authenticateAdmin() throws Exception {
        adminMvc = TestAdminMockMvc.withAdmin(context, mockMvc, objectMapper);
        adminToken = loginToken("admin", "Admin@123");
    }

    @Test
    void demoAccountCanUseTheSystemWithoutChangingItsPassword() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content("""
                        {"username":"admin","password":"Admin@123"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.mustChangePassword").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();
        mockMvc.perform(get("/api/data/students").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void enforcesRegistrationApprovalAndRoleScopedLeaveWorkflow() throws Exception {
        mockMvc.perform(get("/api/data/students"))
                .andExpect(status().isUnauthorized());

        String teacherUsername = "teacher_" + UUID.randomUUID().toString().substring(0, 8);
        adminMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content("""
                        {"role":"TEACHER","personId":"T002","phone":"13900000002",
                         "username":"%s","password":"TeachPass@123"}
                        """.formatted(teacherUsername)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pendingApproval").value(true));

        mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content("""
                        {"username":"%s","password":"TeachPass@123"}
                        """.formatted(teacherUsername)))
                .andExpect(status().isForbidden());

        adminMvc.perform(patch("/api/auth/accounts/{username}", teacherUsername)
                        .contentType(APPLICATION_JSON).content("""
                                {"status":"ACTIVE"}
                                """))
                .andExpect(status().isNoContent());
        String teacherToken = loginToken(teacherUsername, "TeachPass@123");

        mockMvc.perform(get("/api/data/classes").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("K002"));
        mockMvc.perform(post("/api/schedule-items").header("Authorization", "Bearer " + teacherToken)
                        .contentType(APPLICATION_JSON).content("""
                                {"course":"无权排课","className":"数学思维 B 班","teacher":"张伟",
                                 "room":"教室 205","campus":"中关村","weekday":"周一",
                                 "startTime":"18:00","endTime":"19:00","type":"常规课"}
                                """))
                .andExpect(status().isForbidden());

        String studentUsername = "student_" + UUID.randomUUID().toString().substring(0, 8);
        adminMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content("""
                        {"role":"STUDENT","personId":"S002","phone":"13800000002",
                         "username":"%s","password":"StudPass@123"}
                        """.formatted(studentUsername)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pendingApproval").value(false));
        String studentToken = loginToken(studentUsername, "StudPass@123");
        mockMvc.perform(get("/api/data/students").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("S002"));

        String leaveResponse = mockMvc.perform(post("/api/auth/leave-requests")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(APPLICATION_JSON).content("""
                                {"type":"STUDENT_LEAVE","classId":"K002","leaveDate":"%s",
                                 "reason":"身体不适"}
                                """.formatted(LocalDate.now().plusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String leaveId = objectMapper.readTree(leaveResponse).get("id").asText();

        mockMvc.perform(get("/api/auth/leave-requests").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(patch("/api/auth/leave-requests/{id}", leaveId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(APPLICATION_JSON).content("""
                                {"status":"APPROVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(patch("/api/auth/leave-requests/{id}", leaveId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(APPLICATION_JSON).content("""
                                {"status":"REJECTED"}
                                """))
                .andExpect(status().isForbidden());
    }

    private String loginToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("token").asText();
    }
}
