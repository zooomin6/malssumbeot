package com.malssumbeot.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 기본(운영) 프로파일에서는 {@code /api/auth/dev-token}이 존재하지 않아야 한다 —
 * 실수로 배포 환경에 노출되지 않는 것이 이 기능의 핵심 안전 요건이다.
 *
 * DevAuthController가 빠지면 이 경로는 AuthController의 {@code /{provider}} 패턴에 걸려
 * "dev-token"을 알 수 없는 제공자로 처리한다 — 그래서 404가 아니라 400을 반환한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DevAuthControllerDisabledByDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 엔드포인트가_존재하지_않아_알_수_없는_제공자로_처리된다() throws Exception {
        mockMvc.perform(post("/api/auth/dev-token"))
                .andExpect(status().isBadRequest());
    }
}
