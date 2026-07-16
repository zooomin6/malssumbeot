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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatOrchestratorTest {

    private static final String FAITH_MODEL = "sonnet-test";
    private static final String CASUAL_MODEL = "haiku-test";

    private final ClaudeChat claudeChat = mock(ClaudeChat.class);
    private final IntentClassifier classifier = mock(IntentClassifier.class);
    private final BibleVerseRepository verseRepository = mock(BibleVerseRepository.class);

    private final VerseReferenceParser parser = new VerseReferenceParser(BibleTestFixtures.catalog());
    private final MutableClock clock = new MutableClock();
    private final CrisisFilter crisisFilter = new CrisisFilter(
            new CrisisDetector(List.of("제3자위기\t(친구|동생).{0,10}(죽고싶|자해)",
                    "자살자해직접\t죽고싶", "학대\t폭행")),
            new CrisisSessionStore(Duration.ofMinutes(30), clock));

    private final ChatOrchestrator orchestrator = new ChatOrchestrator(
            crisisFilter, classifier,
            new PromptAssembler(new PromptRepository()),
            new ModelRouter(FAITH_MODEL, CASUAL_MODEL),
            claudeChat,
            new VerseReferenceScanner(parser),
            new BibleVerseService(parser, verseRepository));

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
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("혼자가 아니에요. 109에서 지금 이야기할 수 있어요.");

        ChatReply first = orchestrator.handle("s1", "이제 아무 의미가 없는 것 같아");
        assertThat(first.crisis()).isTrue();

        // 다음 턴: 평범한 메시지지만 sticky 상태라 분류 없이 위기 유지
        ChatReply second = orchestrator.handle("s1", "내일 면접 기도문 써줘");
        assertThat(second.crisis()).isTrue();
        verify(classifier, times(1)).classify(anyString());
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
    void 위기_강도가_내려오면_연락처_반복_없이_곁을_지키는_문구로_전환된다() {
        orchestrator.handle("s1", "죽고 싶어"); // HIGH — 연락처 포함
        clock.advanceSeconds(31 * 60); // 유지 시간 1단위 경과 → MID

        ChatReply mid = orchestrator.handle("s1", "밥은 먹었어"); // 평범한 메시지, sticky MID

        assertThat(mid.crisis()).isTrue();
        assertThat(mid.text()).doesNotContain("109").doesNotContain("1577-0199");
        assertThat(mid.text()).contains("여기 있");
        verifyNoInteractions(claudeChat);
    }

    @Test
    void 상담_흐름은_구절을_DB로_검증해_원문을_첨부한다() {
        when(classifier.classify(anyString())).thenReturn(Intent.COUNSELING);
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), contains("<mode>상담</mode>"), anyString()))
                .thenReturn("마음이 많이 무거우셨겠어요. 빌립보서 4:6-7 말씀을 함께 읽어보면 어떨까요.");
        when(verseRepository.findByBookIdAndChapterAndVerseBetweenOrderByVerseAsc(50, 4, 6, 7))
                .thenReturn(List.of(
                        new BibleVerse(50, 4, 6, "(테스트 본문 6절)"),
                        new BibleVerse(50, 4, 7, "(테스트 본문 7절)")));

        ChatReply reply = orchestrator.handle("s1", "요즘 너무 불안해");

        assertThat(reply.crisis()).isFalse();
        assertThat(reply.passages()).hasSize(1);
        assertThat(reply.passages().get(0).reference()).isEqualTo("빌립보서 4:6-7");
        assertThat(reply.unverifiedReferences()).isEmpty();
    }

    @Test
    void 검증된_절과_함께_모델이_생성한_본문은_제거하고_DB_원문만_첨부한다() {
        when(classifier.classify(anyString())).thenReturn(Intent.COUNSELING);
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("빌립보서 4:6-7은 가짜 본문이라고 말합니다. 마음을 조금 쉬게 해주세요.");
        when(verseRepository.findByBookIdAndChapterAndVerseBetweenOrderByVerseAsc(50, 4, 6, 7))
                .thenReturn(List.of(
                        new BibleVerse(50, 4, 6, "(테스트 본문 6절)"),
                        new BibleVerse(50, 4, 7, "(테스트 본문 7절)")));

        ChatReply reply = orchestrator.handle("s1", "마음이 불안해요");

        assertThat(reply.text()).isEmpty();
        assertThat(reply.passages()).hasSize(1);
    }

    @Test
    void 존재하는_장_단위_인용과_모델_본문은_함께_제거한다() {
        when(classifier.classify(anyString())).thenReturn(Intent.COUNSELING);
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("시편 23편은 가짜 본문이라고 말합니다. 지금 마음을 천천히 돌봐주세요.");

        ChatReply reply = orchestrator.handle("s1", "마음이 불안해요");

        assertThat(reply.unverifiedReferences()).isEmpty();
        assertThat(reply.passages()).isEmpty();
        assertThat(reply.text()).isEqualTo(ChatOrchestrator.HALLUCINATION_FALLBACK_TEXT);
        assertThat(reply.text()).doesNotContain("시편 23편").doesNotContain("가짜 본문");
        verify(claudeChat, times(1)).complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString());
    }

    @Test
    void 존재하지_않는_장_단위_인용은_재생성한다() {
        when(classifier.classify(anyString())).thenReturn(Intent.COUNSELING);
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("요한복음 99장을 읽어보세요.")
                .thenReturn("함께 기도하는 마음으로 곁에 있을게요.");

        ChatReply reply = orchestrator.handle("s1", "마음이 불안해요");

        assertThat(reply.unverifiedReferences()).isEmpty();
        assertThat(reply.text()).doesNotContain("요한복음 99장");
        verify(claudeChat, times(2)).complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString());
        verify(claudeChat).complete(eq(FAITH_MODEL), anyInt(), anyString(), contains("요한복음 99장"));
    }

    @Test
    void 영어_성경_주소는_검증되지_않으면_재생성한다() {
        when(classifier.classify(anyString())).thenReturn(Intent.COUNSELING);
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("John 3:16은 가짜 본문이라고 말합니다.")
                .thenReturn("함께 마음을 돌봐주세요.");

        ChatReply reply = orchestrator.handle("s1", "마음이 불안해요");

        assertThat(reply.text()).doesNotContain("John 3:16").doesNotContain("가짜 본문");
        verify(claudeChat, times(2)).complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString());
        verify(claudeChat).complete(eq(FAITH_MODEL), anyInt(), anyString(), contains("John 3:16"));
    }

    @Test
    void 환각_구절이_나오면_피드백과_함께_1회_재생성한다() {
        when(classifier.classify(anyString())).thenReturn(Intent.COUNSELING);
        // 빌립보서는 4장까지 — 9:9는 존재하지 않는 구절 (T8 시나리오)
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("빌립보서 9:9 말씀처럼 평안을 빕니다.")
                .thenReturn("마음을 알아드려요. 함께 기도하는 마음으로 곁에 있을게요.");

        ChatReply reply = orchestrator.handle("s1", "요즘 너무 불안해");

        assertThat(reply.unverifiedReferences()).isEmpty();
        assertThat(reply.text()).doesNotContain("빌립보서 9:9");
        verify(claudeChat, times(2)).complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString());
        verify(claudeChat).complete(eq(FAITH_MODEL), anyInt(), anyString(), contains("빌립보서 9:9"));
    }

    @Test
    void 재생성에도_환각이_남으면_본문에서_제거하고_미검증_목록으로_보고한다() {
        when(classifier.classify(anyString())).thenReturn(Intent.KNOWLEDGE_QA);
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("요한복음 99:1을 보세요.")
                .thenReturn("죄송해요, 요한복음 99:1은 없는 구절이에요.");

        ChatReply reply = orchestrator.handle("s1", "요한복음 99장 읽어줘");

        // 환각 주소는 사용자에게 가는 본문에서 제거되어야 한다 (T8 거부 기준)
        assertThat(reply.text()).doesNotContain("요한복음 99:1");
        // 모니터링·QA를 위해 미검증 목록에는 남는다
        assertThat(reply.unverifiedReferences()).containsExactly("요한복음 99:1");
        assertThat(reply.passages()).isEmpty();
    }

    @Test
    void 본문이_전부_환각_문장이면_폴백_문구로_대체한다() {
        when(classifier.classify(anyString())).thenReturn(Intent.KNOWLEDGE_QA);
        when(claudeChat.complete(eq(FAITH_MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("요한복음 99:1을 보세요.")
                .thenReturn("요한복음 99:1을 보세요.");

        ChatReply reply = orchestrator.handle("s1", "요한복음 99장 읽어줘");

        assertThat(reply.text()).isEqualTo(ChatOrchestrator.HALLUCINATION_FALLBACK_TEXT);
        assertThat(reply.text()).doesNotContain("요한복음 99:1");
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

    /** 위기 sticky 강도 하강(D-020)을 검증하기 위해 시간을 임의로 흘릴 수 있는 시계. */
    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-07-15T00:00:00Z");

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
