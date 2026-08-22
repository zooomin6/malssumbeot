package com.malssumbeot.archive;

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
import java.time.Instant;
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

@WebMvcTest(ArchiveItemController.class)
@Import({JwtService.class, RateLimitConfig.class})
class ArchiveItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ArchiveItemService service;

    private String tokenFor(long userId) {
        User user = new User(AuthProvider.GOOGLE, "pid-" + userId, null, null);
        ReflectionTestUtils.setField(user, "id", userId);
        return jwtService.issue(user);
    }

    @Test
    void 토큰이_없으면_401이고_서비스를_부르지_않는다() throws Exception {
        mockMvc.perform(post("/api/archive-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barnabasReply\":\"답변\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void 빈_barnabasReply는_400() throws Exception {
        mockMvc.perform(post("/api/archive-items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barnabasReply\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 생성하면_201과_본문을_반환한다() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(
                new ArchiveItemResponse(1L, "질문", "답변", List.of(), Instant.now()));

        mockMvc.perform(post("/api/archive-items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userMessage\":\"질문\",\"barnabasReply\":\"답변\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barnabasReply").value("답변"));
    }

    @Test
    void 목록_조회는_토큰의_사용자_id로_서비스를_부른다() throws Exception {
        when(service.findMine(eq(7L))).thenReturn(List.of());

        mockMvc.perform(get("/api/archive-items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void 삭제는_204() throws Exception {
        mockMvc.perform(delete("/api/archive-items/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(1L)))
                .andExpect(status().isNoContent());
    }
}
