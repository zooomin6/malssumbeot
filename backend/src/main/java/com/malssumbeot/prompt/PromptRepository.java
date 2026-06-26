package com.malssumbeot.prompt;

import com.malssumbeot.orchestrator.Intent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 시스템 프롬프트 리소스 로더. 프롬프트 내용 수정은 사람 승인 필요 항목이므로
 * 코드가 아닌 resources/prompts/*.txt 파일로 관리한다 (diff 검토 용이).
 */
@Component
public class PromptRepository {

    private final String master;
    private final String crisis;
    private final Map<Intent, String> byIntent;

    public PromptRepository() {
        this.master = load("master.txt");
        this.crisis = load("crisis.txt");
        Map<Intent, String> modes = new EnumMap<>(Intent.class);
        modes.put(Intent.COUNSELING, load("counseling.txt"));
        modes.put(Intent.PRAYER, load("prayer.txt"));
        modes.put(Intent.KNOWLEDGE_QA, load("knowledge-qa.txt"));
        modes.put(Intent.DAILY_CHAT, load("daily-chat.txt"));
        modes.put(Intent.OUT_OF_SCOPE, load("out-of-scope.txt"));
        modes.put(Intent.CRISIS, this.crisis);
        this.byIntent = Map.copyOf(modes);
    }

    public String master() {
        return master;
    }

    public String crisis() {
        return crisis;
    }

    public String forIntent(Intent intent) {
        return byIntent.get(intent);
    }

    private static String load(String fileName) {
        try {
            return new ClassPathResource("prompts/" + fileName)
                    .getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("프롬프트 파일을 읽을 수 없습니다: " + fileName, e);
        }
    }
}
