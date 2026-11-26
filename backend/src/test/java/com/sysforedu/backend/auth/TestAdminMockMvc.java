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
        var login = original.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk()).andReturn().getResponse();
        JsonNode response = objectMapper.readTree(login.getContentAsString());
        String token = response.get("token").asText();
        return MockMvcBuilders.webAppContextSetup(context)
                .defaultRequest(get("/").header("Authorization", "Bearer " + token))
                .build();
    }

    public static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }
}
