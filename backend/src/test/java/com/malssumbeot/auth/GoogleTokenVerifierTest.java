package com.malssumbeot.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * 실제 구글 검증기로 형식이 깨진 토큰을 검증한다 — 파싱 단계에서 IllegalArgumentException이 나며,
 * 이를 InvalidSocialTokenException(→401)으로 변환하는지 확인한다. (스모크 테스트에서 500으로 샌 경로의 회귀 방지)
 * 네트워크 호출 없이 파싱 단계에서 실패하므로 구글 서버에 붙지 않는다.
 */
class GoogleTokenVerifierTest {

    @Test
    void 형식이_깨진_토큰은_401_예외로_변환한다() {
        GoogleIdTokenVerifier googleVerifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList("test-client-id"))
                .build();
        GoogleTokenVerifier verifier = new GoogleTokenVerifier(googleVerifier);

        assertThatThrownBy(() -> verifier.verify("not-a-real-jwt"))
                .isInstanceOf(InvalidSocialTokenException.class);
    }
}
