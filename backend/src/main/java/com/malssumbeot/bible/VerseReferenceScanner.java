package com.malssumbeot.bible;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 모델 응답 텍스트에서 구절 주소 후보를 찾아낸다 (구절 검증 파이프라인의 입력).
 *
 * 책 이름이 카탈로그에서 해석되는 후보만 반환한다 — "오후 3:30" 같은
 * 시각 표현은 책 이름 해석에 실패하므로 자연히 제외된다.
 */
public class VerseReferenceScanner {

    private static final Pattern CANDIDATE = Pattern.compile(
            "([가-힣]{1,10})\\s?(\\d{1,3})\\s*:\\s*(\\d{1,3})(?:\\s*[-~]\\s*(\\d{1,3}))?");

    private final VerseReferenceParser parser;

    public VerseReferenceScanner(VerseReferenceParser parser) {
        this.parser = parser;
    }

    /** 텍스트에서 해석 가능한 구절 주소 문자열 목록(중복 제거, 등장 순서)을 돌려준다. */
    public List<String> scan(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = CANDIDATE.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group().strip();
            try {
                parser.parse(candidate);
                found.add(candidate);
            } catch (InvalidVerseReferenceException e) {
                // 책 이름이 아니거나 형식이 아님 — 구절 주소로 취급하지 않는다
            }
        }
        return new ArrayList<>(found);
    }
}
