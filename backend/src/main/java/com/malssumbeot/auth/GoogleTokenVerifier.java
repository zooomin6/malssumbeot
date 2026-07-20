package com.malssumbeot.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.malssumbeot.user.AuthProvider;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.stereotype.Component;

/**
 * 구글 ID 토큰 검증. 구글 라이브러리가 서명(구글 공개키)·audience(우리 client-id)·만료를 확인한다.
 * verify가 null을 돌려주면(검증 실패) 유효하지 않은 토큰이다.
 */
@Component
public class GoogleTokenVerifier implements SocialTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public SocialUser verify(String token) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(token);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            // IllegalArgumentException: 토큰이 JWT 형식조차 아닐 때 파싱 단계에서 발생
            throw new InvalidSocialTokenException("구글 ID 토큰 검증에 실패했습니다.", e);
        }
        if (idToken == null) {
            throw new InvalidSocialTokenException("유효하지 않은 구글 ID 토큰입니다.");
        }
        GoogleIdToken.Payload payload = idToken.getPayload();
        return new SocialUser(AuthProvider.GOOGLE, payload.getSubject(),
                payload.getEmail(), (String) payload.get("name"));
    }
}
