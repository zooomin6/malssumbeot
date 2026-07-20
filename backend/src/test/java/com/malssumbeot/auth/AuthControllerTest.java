package com.malssumbeot.auth;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.malssumbeot.user.AuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨트롤러 슬라이스: 제공자 경로 매핑·입력 검증만 검증한다 (AuthService는 모킹).
 * 전역 인증 인터셉터(WebConfig)가 슬라이스에 로드되며 JwtService에 의존하므로 임포트한다 —
 * {@code /api/auth/**}는 인터셉터 제외 경로라 인증 검사는 돌지 않는다.
 */
@WebMvcTest(AuthController.class)
@Import(JwtService.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void 구글_로그인_응답을_JSON으로_매핑한다() throws Exception {
        when(authService.login(eq(AuthProvider.GOOGLE), eq("tok")))
                .thenReturn(new LoginResponse("jwt-1", "GOOGLE", "민규", "a@example.com"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-1"))
                .andExpect(jsonPath("$.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.nickname").value("민규"));
    }

    @Test
    void 빈_토큰은_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 알_수_없는_제공자는_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/facebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\"}"))
                .andExpect(status().isBadRequest());
    }
}
