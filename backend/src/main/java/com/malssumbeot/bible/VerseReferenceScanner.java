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
 * 한글 후보는 책 이름이 카탈로그에서 해석되는 경우만 반환한다. 영어 장:절 표기는
 * DB 책 이름으로 해석하지 못해도 안전상 환각 후보로 반환한다.
 *
 * 한글은 정규식만으로 시각("오후 3:30")·점수("12:7") 표기를 걸러낼 수 없어, 책 이름 해석
 * 성공 여부를 그 필터로 겸해 쓴다 — 그래서 영어와 달리 해석 실패 시 후보를 버린다. 이 때문에
 * 카탈로그에 없는 한글 "가짜 책 이름"(예: 에녹서)은 검증을 우회한다 — 알려진 한계 (PROGRESS.md 참고).
 */
public class VerseReferenceScanner {

    private static final String KOREAN_REFERENCE =
            "[가-힣]{1,10}\\s?\\d{1,3}(?:\\s*:\\s*\\d{1,3}(?:\\s*[-~]\\s*\\d{1,3})?"
                    + "|\\s*장\\s*\\d{1,3}(?:\\s*[-~]\\s*\\d{1,3})?\\s*절?"
                    + "|\\s*(?:장|편))";
    private static final String ENGLISH_REFERENCE =
            "(?!(?i:am|pm)\\s)(?:[1-3]\\s*)?[A-Za-z]{2,20}\\s+\\d{1,3}\\s*:\\s*\\d{1,3}"
                    + "(?:\\s*[-~]\\s*\\d{1,3})?";
    private static final Pattern CANDIDATE =
            Pattern.compile("(?:" + KOREAN_REFERENCE + "|" + ENGLISH_REFERENCE + ")");
    private static final Pattern ENGLISH_CANDIDATE = Pattern.compile("^" + ENGLISH_REFERENCE + "$");

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
            if (ENGLISH_CANDIDATE.matcher(candidate).matches()) {
                found.add(candidate);
                continue;
            }
            try {
                if (candidate.endsWith("장") || candidate.endsWith("편")) {
                    parser.parseChapter(candidate);
                } else {
                    parser.parse(candidate);
                }
                found.add(candidate);
            } catch (InvalidVerseReferenceException e) {
                // 책 이름이 아니거나 형식이 아님 — 구절 주소로 취급하지 않는다
            }
        }
        return new ArrayList<>(found);
    }
}
