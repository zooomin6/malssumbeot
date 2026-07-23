package com.malssumbeot.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
 * 컨트롤러 슬라이스: HTTP 매핑과 입력 검증만 검증한다 (오케스트레이터는 모킹).
 * {@code /api/chat}은 JWT 인증 게이트 뒤라, 요청마다 유효한 토큰을 실어 게이트를 통과시킨 뒤 매핑을 본다.
 */
@WebMvcTest(ChatController.class)
@Import({JwtService.class, RateLimitConfig.class})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ChatOrchestrator orchestrator;

    private String bearer() {
        return "Bearer " + jwtService.issue(new User(AuthProvider.GOOGLE, "pid-1", null, null));
    }

    @Test
    void 일반_대화_응답을_JSON으로_매핑한다() throws Exception {
        when(orchestrator.handle(eq("s1"), any()))
                .thenReturn(new ChatReply("평안하세요.", Intent.DAILY_CHAT, false, List.of(), List.of()));

        mockMvc.perform(post("/api/chat")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"안녕하세요\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("평안하세요."))
                .andExpect(jsonPath("$.intent").value("DAILY_CHAT"))
                .andExpect(jsonPath("$.crisis").value(false));
    }

    @Test
    void 빈_메시지는_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sessionId가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"안녕하세요\"}"))
                .andExpect(status().isBadRequest());
    }
}
