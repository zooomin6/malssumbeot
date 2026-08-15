package com.malssumbeot.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** dev 프로파일에서 {@code /api/auth/dev-token}이 토큰을 정상 발급하는지 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 토큰을_발급한다() throws Exception {
        mockMvc.perform(post("/api/auth/dev-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("DEV"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
