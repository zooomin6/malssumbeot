package com.malssumbeot.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthConfig.class);

    /**
     * 구글 ID 토큰 검증기. audience를 우리 client-id로 고정해 다른 앱에 발급된 토큰을 거부한다.
     * client-id 미설정 시에도 부팅은 되지만(빈 audience) 실제 검증은 실패한다 (AnthropicConfig와 동일 철학).
     */
    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(
            @Value("${malssumbeot.auth.google.client-id:}") String clientId) {
        if (clientId == null || clientId.isBlank()) {
            log.warn("GOOGLE_CLIENT_ID가 설정되지 않았습니다. 구글 로그인 검증은 실패합니다.");
        }
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }
}
