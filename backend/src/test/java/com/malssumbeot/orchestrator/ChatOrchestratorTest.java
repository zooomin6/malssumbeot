package com.malssumbeot.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.malssumbeot.bible.BibleTestFixtures;
import com.malssumbeot.bible.BibleVerse;
import com.malssumbeot.bible.BibleVerseRepository;
import com.malssumbeot.bible.BibleVerseService;
import com.malssumbeot.bible.VerseReferenceParser;
import com.malssumbeot.bible.VerseReferenceScanner;
import com.malssumbeot.crisis.CrisisDetector;
import com.malssumbeot.crisis.CrisisFilter;
import com.malssumbeot.crisis.CrisisSessionStore;
import com.malssumbeot.prompt.PromptAssembler;
import com.malssumbeot.prompt.PromptRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatOrchestratorTest {

    private static final String FAITH_MODEL = "sonnet-test";
    private static final String CASUAL_MODEL = "haiku-test";
    private static final String PROPOSAL_MODEL = "haiku-proposal-test";

    private final ClaudeChat claudeChat = mock(ClaudeChat.class);
    private final IntentClassifier classifier = mock(IntentClassifier.class);
    private final BibleVerseRepository verseRepository = mock(BibleVerseRepository.class);

    private final VerseReferenceParser parser = new VerseReferenceParser(BibleTestFixtures.catalog());
    private final CrisisFilter crisisFilter = new CrisisFilter(
            new CrisisDetector(List.of("제3자위기\t(친구|동생).{0,10}(죽고싶|자해)",
                    "자살자해직접\t죽고싶", "학대\t폭행")),
            new CrisisSessionStore());

    private final ChatOrchestrator orchestrator = new ChatOrchestrator(
            crisisFilter, classifier,
            new PromptAssembler(new PromptRepository()), new PromptRepository(),
            new ModelRouter(FAITH_MODEL, CASUAL_MODEL),
            claudeChat,
            new VerseReferenceScanner(parser),
            new BibleVerseService(parser, verseRepository),
            PROPOSAL_MODEL);

    @Test
    void 위기_메시지는_분류를_거치지_않고_위기_프로토콜로_간다() {
        ChatReply reply = orchestrator.handle("s1", "그냥 죽고 싶어");

        assertThat(reply.crisis()).isTrue();
        assertThat(reply.intent()).isEqualTo(Intent.CRISIS);
        assertThat(reply.text()).contains("109").contains("1577-0199");
        verifyNoInteractions(classifier, claudeChat);
    }

    @Test
    void 위기_응답은_모델_호출_없이_결정론적_안내를_보낸다() {
        ChatReply reply = orchestrator.handle("s1", "죽고 싶어");

        assertThat(reply.crisis()).isTrue();
        assertThat(reply.text()).contains("109").contains("1577-0199");
        verifyNoInteractions(claudeChat);
    }

    @Test
    void 분류기의_위기_판정도_세션을_위기_상태로_만든다() {
        when(classifier.classify("이제 아무 의미가 없는 것 같아")).thenReturn(Intent.CRISIS);

        ChatReply first = orchestrator.handle("s1", "이제 아무 의미가 없는 것 같아");
        assertThat(first.crisis()).isTrue();

        // 다음 턴: 평범한 메시지지만 sticky 상태라 분류 없이 위기 유지
        ChatReply second = orchestrator.handle("s1", "내일 면접 기도문 써줘");
        assertThat(second.crisis()).isTrue();
        verify(classifier, times(1)).classify(anyString());
        verifyNoInteractions(claudeChat);
    }

    @Test
    void 자살자해_위기는_믿을만한_사람_권유를_포함한다() {
        ChatReply reply = orchestrator.handle("s1", "죽고 싶어");

        assertThat(reply.crisis()).isTrue();
        assertThat(reply.text()).contains("믿을 만한 사람");
        assertThat(reply.text()).contains("109").contains("1577-0199");
    }

    @Test
    void 학대_위기는_믿을만한_사람_권유_없이_안내한다() {
        ChatReply reply = orchestrator.handle("s1", "집에서 폭행을 당하고 있어요");

        assertThat(reply.crisis()).isTrue();
        assertThat(reply.text()).doesNotContain("믿을 만한 사람");
        assertThat(reply.text()).contains("112").contains("1366");
        verifyNoInteractions(claudeChat);
    }

    @Test
    void 제3자_위기는_당사자용이_아닌_제3자용_문구로_안내한다() {
        ChatReply reply = orchestrator.handle("s1", "친구가 죽고 싶대요");

        assertThat(reply.crisis()).isTrue();
        assertThat(reply.text()).contains("가까운 분");
        assertThat(reply.text()).doesNotContain("믿을 만한 사람");
        assertThat(reply.text()).contains("112");
        verifyNoInteractions(claudeChat);
    }

    @Test
    void 위기_직후_한_번은_평범해도_위기_문구를_유지하고_그_다음부터는_새_요청대로_처리한다() {
        orchestrator.handle("s1", "죽고 싶어"); // 위기 신호

        ChatReply second = orchestrator.handle("s1", "밥은 먹었어"); // sticky 1회 소비(D-026)
        assertThat(second.crisis()).isTrue();

        when(classifier.classify(anyString())).thenReturn(Intent.DAILY_CHAT);
        when(claudeChat.complete(eq(CASUAL_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("오늘도 평안하시길요!");

        ChatReply third = orchestrator.handle("s1", "오늘 날씨 어때?"); // sticky 이미 소비됨

        assertThat(third.crisis()).isFalse();
        assertThat(third.text()).isEqualTo("오늘도 평안하시길요!");
    }

    @Test
    void 상담_흐름은_1단계에서_검증된_구절_원문을_보고_코멘트를_생성한다() {
        when(classifier.classify(anyString())).thenReturn(Intent.COUNSELING);
        when(claudeChat.complete(eq(PROPOSAL_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("빌립보서 4:6-7");
        when(verseRepository.findByBookIdAndChapterAndVerseBetweenOrderByVerseAsc(50, 4, 6, 7))
                .thenReturn(List.of(
                        new BibleVerse(50, 4, 6, "(테스트 본문 6절)"),
                        new BibleVerse(50, 4, 7, "(테스트 본문 7절)")));
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), contains("<bible_verses>"), anyString()))
                .thenReturn("마음이 많이 무거우셨겠어요. 빌립보서 4:6-7 말씀을 함께 읽어보면 어떨까요.");

        ChatReply reply = orchestrator.handle("s1", "요즘 너무 불안해");

        assertThat(reply.crisis()).isFalse();
        // P0-3 핵심: 검증된 구절이 있어도 모델의 공감 코멘트가 삭제되지 않고 그대로 남는다
        assertThat(reply.text()).contains("마음이 많이 무거우셨겠어요");
        assertThat(reply.passages()).hasSize(1);
        assertThat(reply.passages().get(0).reference()).isEqualTo("빌립보서 4:6-7");
        assertThat(reply.unverifiedReferences()).isEmpty();
    }

    @Test
    void 신앙_인텐트인데_검증된_구절이_없으면_인용금지_안내를_담아_생성한다() {
        when(classifier.classify(anyString())).thenReturn(Intent.COUNSELING);
        when(claudeChat.complete(eq(PROPOSAL_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("");
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), contains("단정적 표현을 쓰지 마세요"), anyString()))
                .thenReturn("그 마음 충분히 이해돼요. 곁에서 함께 하고 있을게요.");

        ChatReply reply = orchestrator.handle("s1", "그냥 이런저런 얘기가 하고 싶어");

        assertThat(reply.text()).isEqualTo("그 마음 충분히 이해돼요. 곁에서 함께 하고 있을게요.");
        assertThat(reply.passages()).isEmpty();
    }

    @Test
    void 제공되지_않은_구절_주소는_그_부분만_제거하고_나머지_코멘트는_남긴다() {
        when(classifier.classify(anyString())).thenReturn(Intent.COUNSELING);
        when(claudeChat.complete(eq(PROPOSAL_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("");
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("시편 23편도 함께 읽어보시면 좋겠어요. 지금 마음을 천천히 돌봐주세요.");

        ChatReply reply = orchestrator.handle("s1", "마음이 불안해요");

        assertThat(reply.text()).doesNotContain("시편 23편");
        assertThat(reply.text()).contains("지금 마음을 천천히 돌봐주세요");
        assertThat(reply.unverifiedReferences()).containsExactly("시편 23편");
        assertThat(reply.passages()).isEmpty();
    }

    @Test
    void 검증되지_않는_영어_성경_주소는_그_부분만_제거한다() {
        when(classifier.classify(anyString())).thenReturn(Intent.COUNSELING);
        when(claudeChat.complete(eq(PROPOSAL_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("");
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("John 3:16 말씀처럼, 마음을 조금 쉬게 해주세요.");

        ChatReply reply = orchestrator.handle("s1", "마음이 불안해요");

        assertThat(reply.text()).doesNotContain("John 3:16");
        assertThat(reply.text()).contains("마음을 조금 쉬게 해주세요");
        verify(claudeChat, times(1)).complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString());
    }

    @Test
    void 존재하지_않는_구절_주소는_그_부분만_제거하고_미검증_목록으로_남긴다() {
        when(classifier.classify(anyString())).thenReturn(Intent.KNOWLEDGE_QA);
        when(claudeChat.complete(eq(PROPOSAL_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("");
        // 빌립보서는 4장까지 — 9:9는 존재하지 않는 구절 (T8 시나리오)
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("빌립보서 9:9 말씀처럼 평안을 빕니다.");

        ChatReply reply = orchestrator.handle("s1", "평안이 뭘까요");

        assertThat(reply.text()).doesNotContain("빌립보서 9:9");
        assertThat(reply.unverifiedReferences()).containsExactly("빌립보서 9:9");
        assertThat(reply.passages()).isEmpty();
        verify(claudeChat, times(1)).complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString());
    }

    @Test
    void 신앙_근거가_필요없는_인텐트는_구절_제안_단계를_거치지_않는다() {
        when(classifier.classify(anyString())).thenReturn(Intent.DAILY_CHAT);
        when(claudeChat.complete(eq(CASUAL_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("오늘 하루도 평안하시길요!");

        orchestrator.handle("s1", "안녕!");

        verify(claudeChat, times(0)).complete(eq(PROPOSAL_MODEL), anyInt(), anyString(), anyString());
    }

    @Test
    void 범위_밖_요청은_경량_모델로_라우팅한다() {
        when(classifier.classify(anyString())).thenReturn(Intent.OUT_OF_SCOPE);
        when(claudeChat.complete(eq(CASUAL_MODEL), anyInt(), contains("<mode>범위밖</mode>"), anyString()))
                .thenReturn("그 부분은 제가 도와드리기 어려워요. 대신 고민이나 기도 제목이 있다면 함께할게요.");

        ChatReply reply = orchestrator.handle("s1", "자바 코드 좀 짜줘");

        assertThat(reply.intent()).isEqualTo(Intent.OUT_OF_SCOPE);
        verify(claudeChat).complete(eq(CASUAL_MODEL), anyInt(), anyString(), anyString());
    }
}
