package com.antra.report.client;

import com.antra.report.client.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    @Test
    public void reportApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/report"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteReportRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/report/Req-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void loginWithValidCredentialsReturnsToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    public void loginWithBadCredentialsIsRejected() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void reportApiAccessibleWithValidToken() throws Exception {
        String token = jwtService.generateToken("admin");
        mockMvc.perform(get("/report").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    public void malformedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/report").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void staticUiAndLoginArePublic() throws Exception {
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
    }
}
