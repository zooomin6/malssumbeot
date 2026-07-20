package com.malssumbeot.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.malssumbeot.user.AuthProvider;
import com.malssumbeot.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.Date;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long";

    @Test
    void 발급한_JWT를_파싱하면_사용자_신원이_담겨있다() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(user.getProvider()).thenReturn(AuthProvider.GOOGLE);
        JwtService jwtService = new JwtService(SECRET, Duration.ofHours(1));

        String token = jwtService.issue(user);
        Jws<Claims> parsed = jwtService.parse(token);

        assertThat(parsed.getPayload().getSubject()).isEqualTo("42");
        assertThat(parsed.getPayload().get("provider")).isEqualTo("GOOGLE");
        assertThat(parsed.getPayload().getExpiration()).isAfter(new Date());
    }

    @Test
    void 변조된_토큰은_검증에_실패한다() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getProvider()).thenReturn(AuthProvider.KAKAO);
        JwtService jwtService = new JwtService(SECRET, Duration.ofHours(1));
        String token = jwtService.issue(user);

        assertThatThrownBy(() -> jwtService.parse(token + "tampered"))
                .isInstanceOf(JwtException.class);
    }
}
