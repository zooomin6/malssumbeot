package com.malssumbeot.journal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.malssumbeot.api.RateLimitConfig;
import com.malssumbeot.auth.JwtService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 마음 기록 컨트롤러 슬라이스. JWT subject(authUserId)를 실제 Long으로 파싱해 서비스에 넘기는
 * 것이 이번 마일스톤의 신규 배선이라, 그 경로를 최우선으로 검증한다.
 */
@WebMvcTest(MindRecordController.class)
@Import({JwtService.class, RateLimitConfig.class})
class MindRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private MindRecordService service;

    private String tokenFor(long userId) {
        User user = new User(AuthProvider.GOOGLE, "pid-" + userId, null, null);
        ReflectionTestUtils.setField(user, "id", userId);
        return jwtService.issue(user);
    }

    @Test
    void 토큰이_없으면_401이고_서비스를_부르지_않는다() throws Exception {
        mockMvc.perform(post("/api/mind-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"감사해요\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void 빈_content는_400() throws Exception {
        mockMvc.perform(post("/api/mind-records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 생성하면_201과_본문을_반환한다() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(new MindRecord(1L, "감사한 마음", "감사한 마음"));

        mockMvc.perform(post("/api/mind-records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"감사한 마음\",\"content\":\"감사한 마음\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mood").value("감사한 마음"));
    }

    @Test
    void 목록_조회는_토큰의_사용자_id로_서비스를_부른다() throws Exception {
        when(service.findMine(eq(7L))).thenReturn(List.of());

        mockMvc.perform(get("/api/mind-records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void 삭제는_204() throws Exception {
        mockMvc.perform(delete("/api/mind-records/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(1L)))
                .andExpect(status().isNoContent());
    }
}
