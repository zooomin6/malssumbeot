package com.malssumbeot.orchestrator;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnthropicConfig {

    private static final Logger log = LoggerFactory.getLogger(AnthropicConfig.class);

    @Bean
    public AnthropicClient anthropicClient(
            @Value("${malssumbeot.anthropic.api-key:}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            // 키 없이도 부팅은 가능하게 둔다 (로컬 DB 작업 등). API 호출 시점에 인증 오류 발생.
            log.warn("ANTHROPIC_API_KEY가 설정되지 않았습니다. Claude API 호출은 실패합니다.");
            apiKey = "missing-api-key";
        }
        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
