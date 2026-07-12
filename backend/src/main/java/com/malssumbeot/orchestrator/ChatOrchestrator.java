package com.malssumbeot.orchestrator;

import com.malssumbeot.bible.BibleVerseService;
import com.malssumbeot.bible.VersePassage;
import com.malssumbeot.bible.VerseReferenceScanner;
import com.malssumbeot.crisis.CrisisCheck;
import com.malssumbeot.crisis.CrisisFilter;
import com.malssumbeot.prompt.PromptAssembler;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 대화 파이프라인: 위기 감지 → 의도 분류 → 프롬프트 분기 → 응답 생성 → 구절 검증.
 * 이 순서는 헌법(CLAUDE.md) 컨벤션이며 변경·우회 금지.
 *
 * 위기 분기가 모든 로직에 우선한다 (D-004): CrisisFilter가 잡으면 분류를 건너뛰고,
 * 분류기(2차 방어선)가 위기로 판정해도 동일하게 위기 프로토콜로 간다.
 */
@Component
public class ChatOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestrator.class);
    private static final int MAX_TOKENS = 1024;

    /**
     * 위기 응답의 결정론적 안전망 — 외부 모델에 의존하지 않고 항상 연락처를 전달한다.
     * 연락처는 PRD §5.5 위기 프로토콜 기준. 문구는 사람 승인 대기 (PROGRESS.md).
     */
    static final String CRISIS_FALLBACK_TEXT = """
            많이 힘드셨겠어요. 혼자 견디지 않으셔도 됩니다.
            지금 바로 이야기를 들어줄 수 있는 곳이 있어요.
            · 자살예방 상담전화 109 (24시간)
            · 정신건강 위기상담 1577-0199
            전문적인 도움과 신앙은 함께 갈 수 있어요. 저도 곁에 있겠습니다.""";

    /**
     * 환각 구절을 모두 걷어낸 뒤 남은 본문이 없을 때의 안전 문구.
     * 잘못된 구절을 전하지 않는 것을 우선하며, 사용자를 정죄하지 않는다. 문구는 사람 승인 대기.
     */
    static final String HALLUCINATION_FALLBACK_TEXT =
            "말씀 한 구절을 정확히 확인하지 못했어요. 잘못된 구절을 전해 드리지 않으려 조심하고 있습니다. "
                    + "조금 더 이야기를 들려주시면 함께 살펴볼게요.";

    private final CrisisFilter crisisFilter;
    private final IntentClassifier classifier;
    private final PromptAssembler promptAssembler;
    private final ModelRouter modelRouter;
    private final ClaudeChat claudeChat;
    private final VerseReferenceScanner scanner;
    private final BibleVerseService verseService;

    public ChatOrchestrator(CrisisFilter crisisFilter, IntentClassifier classifier,
                            PromptAssembler promptAssembler, ModelRouter modelRouter,
                            ClaudeChat claudeChat, VerseReferenceScanner scanner,
                            BibleVerseService verseService) {
        this.crisisFilter = crisisFilter;
        this.classifier = classifier;
        this.promptAssembler = promptAssembler;
        this.modelRouter = modelRouter;
        this.claudeChat = claudeChat;
        this.scanner = scanner;
        this.verseService = verseService;
    }

    public ChatReply handle(String sessionId, String userMessage) {
        CrisisCheck check = crisisFilter.check(sessionId, userMessage);
        if (check.crisis()) {
            return crisisReply();
        }

        Intent intent = classifier.classify(userMessage);
        if (intent == Intent.CRISIS) {
            // 2차 방어선(LLM)의 판정도 세션 sticky 상태에 반영한다
            crisisFilter.recordCrisis(sessionId);
            return crisisReply();
        }

        return generate(intent, userMessage);
    }

    private ChatReply crisisReply() {
        return new ChatReply(CRISIS_FALLBACK_TEXT, Intent.CRISIS, true, List.of(), List.of());
    }

    private ChatReply generate(Intent intent, String userMessage) {
        String model = modelRouter.route(intent);
        String system = promptAssembler.assemble(intent, List.of());

        String text = claudeChat.complete(model, MAX_TOKENS, system, userMessage);
        Verification verification = verify(text);

        if (!verification.unverified().isEmpty()) {
            // 환각 의심(T8): 존재하지 않는 주소를 알려주고 1회 재생성
            log.warn("검증 실패 구절 감지, 재생성: {}", verification.unverified());
            String feedback = userMessage + "\n\n<system_note>방금 응답에 인용한 구절 주소 "
                    + String.join(", ", verification.unverified())
                    + " 은(는) 성경에 존재하지 않습니다. 해당 구절을 인용하지 말고, "
                    + "존재가 확실한 구절만 주소로 제안하거나 구절 없이 다시 답해 주세요.</system_note>";
            text = claudeChat.complete(model, MAX_TOKENS, system, feedback);
            verification = verify(text);
        }

        // 주소와 본문이 문장 경계를 넘어 분리될 수 있어, 주소가 하나라도 있으면 모델 텍스트 전체를
        // 보내지 않는다. 검증된 절은 passages의 DB 원문만 별도 전달한다 (D-017).
        if (!verification.references().isEmpty()) {
            text = verification.passages().isEmpty() ? HALLUCINATION_FALLBACK_TEXT : "";
        }

        return new ChatReply(text, intent, intent == Intent.CRISIS,
                verification.passages(), verification.unverified());
    }

    private Verification verify(String text) {
        List<String> references = scanner.scan(text);
        List<VersePassage> passages = new ArrayList<>();
        List<String> unverified = new ArrayList<>();
        for (String reference : references) {
            var passage = verseService.findPassage(reference);
            if (passage.isPresent()) {
                passages.add(passage.get());
            } else if (!verseService.hasChapter(reference)) {
                unverified.add(reference);
            }
        }
        return new Verification(List.copyOf(references), List.copyOf(passages), List.copyOf(unverified));
    }

    private record Verification(List<String> references, List<VersePassage> passages,
                                List<String> unverified) {
    }
}
