package com.malssumbeot.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.malssumbeot.auth.JwtService;
import com.malssumbeot.orchestrator.ClaudeChat;
import com.malssumbeot.user.AuthProvider;
import com.malssumbeot.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP 경로 전체를 실제 빈으로 태워 위기 우회 불가(D-004)를 E2E로 검증한다.
 * 외부 모델(ClaudeChat)만 모킹해 실호출·비용을 막는다 — 위기 경로는 애초에 모델을 부르지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChatApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaudeChat claudeChat;

    @Autowired
    private JwtService jwtService;

    @Test
    void 위기_메시지는_HTTP_경로에서도_모델없이_위기_프로토콜로_응답한다() throws Exception {
        String token = jwtService.issue(new User(AuthProvider.GOOGLE, "pid-1", null, null));
        mockMvc.perform(post("/api/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"죽고 싶어\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crisis").value(true))
                .andExpect(jsonPath("$.intent").value("CRISIS"))
                .andExpect(jsonPath("$.text").value(containsString("109")));

        // 위기 분기는 의도 분류·응답 생성(모델)을 절대 거치지 않는다 (D-004)
        verifyNoInteractions(claudeChat);
    }
}
