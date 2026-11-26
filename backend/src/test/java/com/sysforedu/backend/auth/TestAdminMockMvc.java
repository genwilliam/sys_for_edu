package com.sysforedu.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestAdminMockMvc {
    private TestAdminMockMvc() {
    }

    public static MockMvc withAdmin(WebApplicationContext context, MockMvc original,
                                    ObjectMapper objectMapper) throws Exception {
        String password = "Admin@123";
        var login = original.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"%s"}
                                """.formatted(password)))
                .andReturn().getResponse();
        if (login.getStatus() == 401) {
            password = "Admin@1234";
            login = original.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                            .content("""
                                    {"username":"admin","password":"%s"}
                                    """.formatted(password)))
                    .andExpect(status().isOk()).andReturn().getResponse();
        }
        JsonNode response = objectMapper.readTree(login.getContentAsString());
        String token = response.get("token").asText();
        if (response.path("user").path("mustChangePassword").asBoolean()) {
            original.perform(post("/api/auth/change-password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"newPassword":"Admin@1234"}
                                    """))
                    .andExpect(status().isNoContent());
            login = original.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                            .content("""
                                    {"username":"admin","password":"Admin@1234"}
                                    """))
                    .andExpect(status().isOk()).andReturn().getResponse();
            token = objectMapper.readTree(login.getContentAsString()).get("token").asText();
        }
        String adminToken = token;
        return MockMvcBuilders.webAppContextSetup(context)
                .defaultRequest(get("/").header("Authorization", "Bearer " + adminToken))
                .build();
    }

    public static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }
}
