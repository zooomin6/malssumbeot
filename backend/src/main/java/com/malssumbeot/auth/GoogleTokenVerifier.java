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
 *
 * IOException(구글 공개키 조회 등 네트워크 실패)과 GeneralSecurityException·IllegalArgumentException
 * (서명 불일치·형식 오류 등 실제 무효 토큰)을 구분한다 — 전자는 토큰과 무관한 일시적 상황이라
 * 401이 아닌 503으로 응답한다.
 */
@Component
public class GoogleTokenVerifier implements SocialTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public AuthProvider provider() { //SocialTokenVerifier 인터페이스의 provider() 메서드를 구현하여 이 검증기가 담당하는 제공자를 반환합니다. 이 경우, 구글 토큰 검증기이므로 AuthProvider.GOOGLE을 반환합니다.
        return AuthProvider.GOOGLE;
    }

    @Override
    public SocialUser verify(String token) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(token);
        } catch (IOException e) {
            // 구글 공개키 조회 등 네트워크 실패 — 토큰이 유효한지와 무관한 일시적 상황
            throw new SocialProviderUnavailableException("구글 인증 서버에 일시적으로 연결할 수 없습니다.", e);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
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
