package com.malssumbeot.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.malssumbeot.auth.JwtService;
import com.malssumbeot.orchestrator.ChatOrchestrator;
import com.malssumbeot.orchestrator.ChatReply;
import com.malssumbeot.orchestrator.Intent;
import com.malssumbeot.user.AuthProvider;
import com.malssumbeot.user.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * JWT 인증 게이트 검증: 보호된 {@code /api/chat}은 유효한 토큰 없이는 401이고, 그 경우 오케스트레이터에
 * 닿지도 않는다. 실제 {@link JwtService}를 임포트해(비밀키 미설정 → 임시 키) 토큰을 발급·검증한다.
 */
@WebMvcTest(ChatController.class)
@Import(JwtService.class)
class ChatAuthInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ChatOrchestrator orchestrator;

    private String validToken() {
        return jwtService.issue(new User(AuthProvider.GOOGLE, "pid-1", null, null));
    }

    @Test
    void 토큰이_없으면_401이고_오케스트레이터를_부르지_않는다() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"안녕하세요\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orchestrator);
    }

    @Test
    void Bearer_형식이_아니면_401() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .header(HttpHeaders.AUTHORIZATION, validToken()) // "Bearer " 접두 없음
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"안녕하세요\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orchestrator);
    }

    @Test
    void 깨진_토큰이면_401() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"안녕하세요\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orchestrator);
    }

    @Test
    void 유효한_토큰이면_통과해_200() throws Exception {
        when(orchestrator.handle(eq("s1"), any()))
                .thenReturn(new ChatReply("평안하세요.", Intent.DAILY_CHAT, false, List.of(), List.of()));

        mockMvc.perform(post("/api/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"안녕하세요\"}"))
                .andExpect(status().isOk());
    }
}
