package com.malssumbeot.orchestrator;

import com.malssumbeot.bible.BibleVerseService;
import com.malssumbeot.bible.VersePassage;
import com.malssumbeot.bible.VerseReferenceScanner;
import com.malssumbeot.crisis.CrisisCheck;
import com.malssumbeot.crisis.CrisisFilter;
import com.malssumbeot.crisis.CrisisSignal;
import com.malssumbeot.prompt.PromptAssembler;
import com.malssumbeot.prompt.PromptRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 대화 파이프라인: 위기 감지 → 의도 분류 → (신앙 인텐트만) 구절 주소 제안·DB 검증 → 프롬프트
 * 조립 → 응답 생성 → 잔여 미검증 구절 제거. 이 순서는 헌법(CLAUDE.md) 컨벤션이며 변경·우회 금지.
 *
 * 위기 분기가 모든 로직에 우선한다 (D-004): CrisisFilter가 잡으면 분류를 건너뛰고,
 * 분류기(2차 방어선)가 위기로 판정해도 동일하게 위기 프로토콜로 간다.
 *
 * 성경 근거 파이프라인은 2단계 grounded 생성(옵션B, D-025)이다: 1단계에서 경량 모델에게 관련
 * 구절 주소만 제안받아 DB로 검증하고, 검증된 원문만 2단계 시스템 프롬프트의 <bible_verses>에
 * 실어 최종 응답을 생성한다 — 모델이 실제 원문을 보고 코멘트를 쓴다(P0-1). 검증된 구절이
 * 없으면 PromptAssembler가 "인용하지 말라"는 안내를 대신 붙인다(P0-2). 2단계 출력에 그래도
 * 미검증 구절이 스캔되면(모델이 기억으로 추가 언급) 그 주소만 제거하고 나머지 코멘트는
 * 그대로 둔다 — 과거 D-017의 "주소 있으면 전체 텍스트 차단" 방식은 이 설계로 대체되었다.
 */
@Component
public class ChatOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestrator.class);
    private static final int MAX_TOKENS = 1024;
    private static final int PROPOSAL_MAX_TOKENS = 128;

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

    private final CrisisFilter crisisFilter;
    private final IntentClassifier classifier;
    private final PromptAssembler promptAssembler;
    private final PromptRepository prompts;
    private final ModelRouter modelRouter;
    private final ClaudeChat claudeChat;
    private final VerseReferenceScanner scanner;
    private final BibleVerseService verseService;
    private final String proposalModel;

    public ChatOrchestrator(CrisisFilter crisisFilter, IntentClassifier classifier,
                            PromptAssembler promptAssembler, PromptRepository prompts,
                            ModelRouter modelRouter, ClaudeChat claudeChat,
                            VerseReferenceScanner scanner, BibleVerseService verseService,
                            @Value("${malssumbeot.anthropic.classifier-model}") String proposalModel) {
        this.crisisFilter = crisisFilter;
        this.classifier = classifier;
        this.promptAssembler = promptAssembler;
        this.prompts = prompts;
        this.modelRouter = modelRouter;
        this.claudeChat = claudeChat;
        this.scanner = scanner;
        this.verseService = verseService;
        this.proposalModel = proposalModel;
    }

    public ChatReply handle(String sessionId, String userMessage) {
        CrisisCheck check = crisisFilter.check(sessionId, userMessage);
        if (check.crisis()) {
            // 새 신호든 sticky 1회 확인(D-026)이든 카테고리(자살자해/학대 등)에 따라 응답을 분기한다.
            String category = check.signal().map(CrisisSignal::category).orElse(null);
            return crisisReply(category);
        }

        Intent intent = classifier.classify(userMessage);
        if (intent == Intent.CRISIS) {
            // 2차 방어선(LLM)의 위기 판정도 sticky 1회 확인 대상으로 기록한다.
            crisisFilter.recordCrisis(sessionId);
            return crisisReply(null);
        }

        return generate(intent, userMessage);
    }

    private ChatReply crisisReply(String category) {
        return new ChatReply(crisisText(category), Intent.CRISIS, true, List.of(), List.of());
    }

    /**
     * 위기 응답 선택. 카테고리별 문구로 분기하며, '믿을 만한 사람' 권유는 자살·자해(본인) 신호에만
     * 넣는다. 제3자 위기·학대·불명확은 각각 별도 문구로, 위험할 수 있는 권유가 섞이지 않게 한다.
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
        List<VersePassage> verifiedPassages = intent.requiresGrounding()
                ? proposeVerifiedPassages(userMessage)
                : List.of();

        String system = promptAssembler.assemble(intent, verifiedPassages);
        String text = claudeChat.complete(model, MAX_TOKENS, system, userMessage);

        List<String> unverifiedReferences = new ArrayList<>();
        String finalText = text;
        for (String candidate : scanner.scan(text)) {
            Optional<VersePassage> resolved = verseService.findPassage(candidate);
            boolean alreadyProvided = resolved.isPresent()
                    && verifiedPassages.stream().anyMatch(p -> samePassage(p, resolved.get()));
            if (!alreadyProvided) {
                // 1단계가 제공하지 않은 구절을 모델이 기억으로 추가 언급함 — 그 주소만 제거한다.
                // 코멘트 전체를 버리지 않는다(D-025, D-017의 전체삭제 대체).
                log.warn("2단계 출력에서 제공되지 않은 구절 주소 감지, 제거: {}", candidate);
                unverifiedReferences.add(candidate);
                finalText = finalText.replace(candidate, "");
            }
        }

        return new ChatReply(finalText, intent, false, verifiedPassages, List.copyOf(unverifiedReferences));
    }

    /**
     * 1단계(옵션B, D-025): 경량 모델에게 사용자 메시지와 관련된 구절 주소만 제안받아 DB로
     * 조회·검증한다. DB에 없는 후보는 조용히 버린다 — 2단계에는 검증된 것만 넘긴다.
     */
    private List<VersePassage> proposeVerifiedPassages(String userMessage) {
        String proposal = claudeChat.complete(
                proposalModel, PROPOSAL_MAX_TOKENS, prompts.verseAddressProposal(), userMessage);
        List<VersePassage> passages = new ArrayList<>();
        for (String line : proposal.lines().toList()) {
            String candidate = line.strip();
            if (!candidate.isBlank()) {
                verseService.findPassage(candidate).ifPresent(passages::add);
            }
        }
        return passages;
    }

    /** 책·장·절 범위가 같으면 같은 구절로 본다 — 모델이 조금 다른 표기로 되풀이해도 식별된다. */
    private static boolean samePassage(VersePassage a, VersePassage b) {
        return a.bookName().equals(b.bookName()) && a.chapter() == b.chapter()
                && a.verseStart() == b.verseStart() && a.verseEnd() == b.verseEnd();
    }
}
