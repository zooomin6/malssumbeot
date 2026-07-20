package com.malssumbeot.auth;

import com.malssumbeot.user.AuthProvider;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 액세스 토큰 검증. 토큰으로 카카오 사용자 정보 API를 호출하며, 토큰이 유효하지 않으면
 * 카카오가 4xx로 거부한다(→ 예외). Client Secret은 필요 없다 (방식 A, D-022).
 *
 * 이메일은 비즈앱 전환 전에는 내려오지 않아 null일 수 있다. 닉네임은 kakao_account.profile 우선,
 * 없으면 properties에서 찾는다 (동의 범위·응답 형태에 따라 위치가 다름).
 */
@Component
public class KakaoTokenVerifier implements SocialTokenVerifier {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String userInfoUri;

    public KakaoTokenVerifier(RestClient.Builder builder,
            @Value("${malssumbeot.auth.kakao.user-info-uri}") String userInfoUri) {
        this.restClient = builder.build();
        this.userInfoUri = userInfoUri;
    }

    @Override
    public AuthProvider provider() {//SocialTokenVerifier 인터페이스의 provider() 메서드를 구현하여 이 검증기가 담당하는 제공자를 반환합니다. 이 경우, 카카오 토큰 검증기이므로 AuthProvider.KAKAO을 반환합니다.
        return AuthProvider.KAKAO;
    }

    @Override
    public SocialUser verify(String accessToken) {
        Map<String, Object> body;
        try {
            body = restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (RestClientException e) {
            throw new InvalidSocialTokenException("카카오 토큰 검증에 실패했습니다.", e);
        }
        if (body == null || body.get("id") == null) {
            throw new InvalidSocialTokenException("유효하지 않은 카카오 토큰입니다.");
        }

        String providerId = String.valueOf(body.get("id"));
        Map<String, Object> account = asMap(body.get("kakao_account"));
        Map<String, Object> profile = account == null ? null : asMap(account.get("profile"));
        Map<String, Object> properties = asMap(body.get("properties"));

        String email = account == null ? null : (String) account.get("email");
        String nickname = profile != null ? (String) profile.get("nickname")
                : properties != null ? (String) properties.get("nickname") : null;

        return new SocialUser(AuthProvider.KAKAO, providerId, email, nickname);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }
}
