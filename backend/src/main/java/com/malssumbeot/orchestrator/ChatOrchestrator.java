package com.malssumbeot.orchestrator;

import com.malssumbeot.bible.BibleVerseService;
import com.malssumbeot.bible.VersePassage;
import com.malssumbeot.bible.VerseReferenceScanner;
import com.malssumbeot.crisis.CrisisCheck;
import com.malssumbeot.crisis.CrisisFilter;
import com.malssumbeot.crisis.CrisisSignal;
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
     * 카테고리별로 분리한다: '곁에 믿을 만한 사람에게 알리라'는 권유는 자살·자해 신호에만 적절하고,
     * 학대(곁의 사람이 가해자일 수 있음)나 카테고리 불명확 시엔 넣지 않는다.
     * 연락처는 PRD §5.5 기준. 문구는 사람 승인 대기 (PROGRESS.md).
     */
    static final String CRISIS_SELF_HARM_TEXT = """
            그런 말이 나올 만큼 오늘 많이 힘드셨나 봐요. 그 마음, 혼자 짊어지지 않으셔도 돼요.
            저는 여기서 계속 이야기 들을게요. 그리고 혹시 곁에 믿을 만한 사람이 있다면, 그 사람에게도 마음을 조금 꺼내 보세요. 혼자보다 함께일 때 견디기가 조금은 더 수월해지거든요.
            지금이든 더 힘든 순간이 오든, 언제든 기댈 수 있는 곳이 있어요.
            · 자살예방 상담전화 109 (24시간)
            · 정신건강 위기상담 1577-0199
            전문적인 도움과 신앙은 함께 갈 수 있어요. 저도 곁에 있겠습니다.""";

    /**
     * 학대·카테고리 불명확·2차(분류기) 위기의 기본 문구. '믿을 만한 사람' 권유 없음.
     * 학대 전용 문구·기관 번호(1366·아동보호전문기관 등)와 진술 데이터 보관 정책은
     * 법률 검토 대기 항목이라, 현재는 안전한 잠정본이다 (PROGRESS.md "사람 확인 필요").
     */
    static final String CRISIS_DEFAULT_TEXT = """
            그런 말이 나올 만큼 많이 힘드셨나 봐요. 그 마음, 혼자 짊어지지 않으셔도 돼요.
            저는 여기서 계속 이야기 들을게요. 지금이든 더 힘든 순간이 오든, 언제든 기댈 수 있는 곳이 있어요.
            · 자살예방 상담전화 109 (24시간)
            · 정신건강 위기상담 1577-0199
            · 지금 위험한 상황이라면 112
            전문적인 도움과 신앙은 함께 갈 수 있어요. 저도 곁에 있겠습니다.""";

    /**
     * 제3자(친구·가족 등)가 위기에 처했다는 걱정·도움 요청에 대한 응답. 당사자가 아니므로 1인칭 위로가
     * 아니라, 걱정하는 사람을 향한 안내로 쓴다. (문구는 사람 승인 대기)
     */
    static final String CRISIS_THIRD_PARTY_TEXT = """
            가까운 분이 그런 말을 해서 많이 걱정되고 당황스러우셨겠어요.
            그분이 지금 당장 자신을 해칠 것 같거나 이미 위험한 행동을 하고 있다면, 혼자 두지 말고 112 또는 119에 도움을 요청해 주세요.
            자살예방 상담전화 109에 함께 연락해 상황을 설명하는 것도 도움이 돼요.
            이 상황을 혼자 감당하거나 책임지려 하지 않으셔도 됩니다.""";

    /**
     * 학대 신호에 대한 응답. '믿을 만한 사람' 권유 없이(곁의 사람이 가해자일 수 있음), 안전 우선 + 적절한
     * 기관 안내. 번호·문구는 사람 승인 및 법률 검토 대상 (PROGRESS.md). 진술 데이터 보관 약속은 하지 않는다.
     */
    static final String CRISIS_ABUSE_TEXT = """
            그런 일을 겪고 계셨다면, 그건 당신의 잘못이 아니에요.
            지금 위험한 상황이라면 안전한 곳으로 피하고 112 또는 119에 연락해 주세요.
            여성폭력·가정폭력은 1366, 청소년이라면 1388에서 24시간 상담과 지원을 받을 수 있어요.
            가해자에게 알려지거나 더 위험해질 수 있는 행동은 피하고, 지금 가장 안전한 방법을 먼저 선택해 주세요.""";

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
            // 위기 카테고리(자살자해/학대)에 따라 응답을 분기한다. sticky는 신호가 없어 기본 문구로 간다.
            return crisisReply(check.signal().map(CrisisSignal::category).orElse(null));
        }

        Intent intent = classifier.classify(userMessage);
        if (intent == Intent.CRISIS) {
            // 2차 방어선(LLM)의 판정도 세션 sticky 상태에 반영한다. 카테고리를 알 수 없어 기본 문구로 간다.
            crisisFilter.recordCrisis(sessionId);
            return crisisReply(null);
        }

        return generate(intent, userMessage);
    }

    private ChatReply crisisReply(String category) {
        return new ChatReply(crisisText(category), Intent.CRISIS, true, List.of(), List.of());
    }

    /**
     * 위기 카테고리별 응답 선택. '믿을 만한 사람' 권유는 확실한 자살·자해(본인) 신호일 때만 나간다.
     * 제3자 위기·학대·불명확은 각각 별도 문구로, 위험할 수 있는 권유가 섞이지 않게 한다.
     */
    private static String crisisText(String category) {
        if (category == null) {
            return CRISIS_DEFAULT_TEXT;
        }
        if (category.startsWith("제3자")) {
            return CRISIS_THIRD_PARTY_TEXT;
        }
        if (category.startsWith("학대")) {
            return CRISIS_ABUSE_TEXT;
        }
        if (category.startsWith("자살자해")) {
            return CRISIS_SELF_HARM_TEXT;
        }
        return CRISIS_DEFAULT_TEXT;
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
