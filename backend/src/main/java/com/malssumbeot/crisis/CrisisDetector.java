package com.malssumbeot.crisis;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 결정론적 위기 신호 감지 (1차 방어선, D-004).
 *
 * 공백을 모두 제거한 메시지에 대해 패턴을 부분 매칭한다 ("죽 고 싶 어" 같은 변형 대응).
 * 재현율 우선: 오탐은 위기 프로토콜의 안내로 흡수 가능하지만 미탐은 돌이킬 수 없다.
 */
public class CrisisDetector {

    private record CompiledPattern(String category, Pattern pattern, String raw) {
    }

    private final List<CompiledPattern> patterns;

    public CrisisDetector(List<String> patternLines) {
        List<CompiledPattern> compiled = new ArrayList<>();
        for (String line : patternLines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] cols = trimmed.split("\t");
            if (cols.length != 2) {
                throw new IllegalArgumentException(
                        "위기 패턴 형식 오류 (카테고리<TAB>정규식): " + trimmed);
            }
            compiled.add(new CompiledPattern(cols[0], Pattern.compile(cols[1]), cols[1]));
        }
        if (compiled.isEmpty()) {
            throw new IllegalStateException("위기 패턴이 비어 있습니다. CrisisFilter가 무력화됩니다.");
        }
        this.patterns = List.copyOf(compiled);
    }

    public Optional<CrisisSignal> detect(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        String normalized = message.replaceAll("\\s+", "");
        for (CompiledPattern cp : patterns) {
            if (cp.pattern().matcher(normalized).find()) {
                return Optional.of(new CrisisSignal(cp.category(), cp.raw()));
            }
        }
        return Optional.empty();
    }
}
