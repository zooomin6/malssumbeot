import { Passage, VerseQuote } from "@/components/VerseQuote";
import { API_BASE_URL } from "@/constants/api";
import { useAuth } from "@/contexts/AuthContext";
import { Ionicons } from "@expo/vector-icons";
import { Stack } from "expo-router";
import { useEffect, useRef, useState } from "react";
import {
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { useReanimatedKeyboardAnimation } from "react-native-keyboard-controller";
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withDelay,
  withRepeat,
  withSequence,
  withTiming,
} from "react-native-reanimated";
import Markdown from "react-native-markdown-display";
import { useSafeAreaInsets } from "react-native-safe-area-context";

/** 선택 가능한 텍스트로 렌더링하도록 라이브러리 기본 규칙을 덮어쓴다. */
const markdownRules = {
  text: (node: any, children: any, parent: any, styles: any, inheritedStyles = {}) => (
    <Text key={node.key} selectable style={[inheritedStyles, styles.text]}>
      {node.content}
    </Text>
  ),
  textgroup: (node: any, children: any, parent: any, styles: any) => (
    <Text key={node.key} selectable style={styles.textgroup}>
      {children}
    </Text>
  ),
};

/** 바나바가 응답을 생성하는 동안 보여주는 점 3개 애니메이션. */
function TypingDots() {
  const dot1 = useSharedValue(0.3);
  const dot2 = useSharedValue(0.3);
  const dot3 = useSharedValue(0.3);

  useEffect(() => {
    const loop = (value: typeof dot1, delay: number) => {
      value.value = withDelay(
        delay,
        withRepeat(withSequence(withTiming(1, { duration: 300 }), withTiming(0.3, { duration: 300 })), -1),
      );
    };
    loop(dot1, 0);
    loop(dot2, 150);
    loop(dot3, 300);
  }, [dot1, dot2, dot3]);

  const style1 = useAnimatedStyle(() => ({ opacity: dot1.value }));
  const style2 = useAnimatedStyle(() => ({ opacity: dot2.value }));
  const style3 = useAnimatedStyle(() => ({ opacity: dot3.value }));

  return (
    <View style={styles.typingRow}>
      <Animated.View style={[styles.typingDot, style1]} />
      <Animated.View style={[styles.typingDot, style2]} />
      <Animated.View style={[styles.typingDot, style3]} />
    </View>
  );
}

/** 챗봇 = 바나바 (D-021). notice는 말풍선이 아닌 테두리 안내 문구. */
type Message = {
  id: string;
  text: string;
  fromBarnabas: boolean;
  notice?: boolean;
  passages?: Passage[];
};

/** 시안의 추천 응답. 탭하면 그 문구를 그대로 보낸다. */
const QUICK_REPLIES = ["짧게 기도해줘", "말씀 더 보기"];

/** 서버 연결 전에도 보이는 고정 안내 문구. 목록을 inverted로 그리므로 최신 메시지가 배열 앞에 온다. */
const INITIAL_MESSAGES: Message[] = [
  {
    id: "1",
    text: "바나바는 정답을 대신 내리기보다, 성경 말씀을 바탕으로 마음을 천천히 살피도록 도와드려요.",
    fromBarnabas: true,
    notice: true,
  },
];

export default function ChatScreen() {
  const { token, logout } = useAuth();
  const [messages, setMessages] = useState<Message[]>(INITIAL_MESSAGES);
  const [draft, setDraft] = useState("");
  const [isSending, setIsSending] = useState(false);
  const insets = useSafeAreaInsets();
  const sessionId = useRef(`session-${Math.random().toString(36).slice(2)}`).current;
  const canSend = draft.trim().length > 0 && token !== null;

  // KeyboardProvider가 창 축소를 가져가므로 아래 여백을 직접 만들어 입력창을 밀어 올린다.
  // height는 키보드가 열릴수록 음수로 커지고, 매 프레임 갱신돼 키보드를 그대로 따라간다.
  // 닫힌 동안에는 하단 제스처바 여백이 대신 자리를 지킨다.
  const { height: keyboardHeight } = useReanimatedKeyboardAnimation();
  const bottomInset = useAnimatedStyle(
    () => ({ paddingBottom: Math.max(-keyboardHeight.value, insets.bottom) }),
    [insets.bottom],
  );

  const sendText = async (text: string) => {
    const trimmed = text.trim();
    if (!trimmed || !token) return;
    setMessages((prev) => [
      { id: `${Date.now()}-${prev.length}`, text: trimmed, fromBarnabas: false },
      ...prev,
    ]);

    setIsSending(true);
    try {
      const res = await fetch(`${API_BASE_URL}/api/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify({ sessionId, message: trimmed }),
      });
      const data = await res.json();
      setMessages((prev) => [
        { id: `${Date.now()}-${prev.length}`, text: data.text, fromBarnabas: true, passages: data.passages },
        ...prev,
      ]);
    } catch (err) {
      console.error("채팅 요청 실패", err);
    } finally {
      setIsSending(false);
    }
  };

  const send = () => {
    sendText(draft);
    setDraft("");
  };

  return (
    <Animated.View style={[styles.screen, bottomInset]}>
      <Stack.Screen
        options={{
          headerTitle: () => (
            <View>
              <Text style={styles.headerTitle}>바나바</Text>
              <Text style={styles.headerSubtitle}>말씀 곁을 함께 걷는 동행자</Text>
            </View>
          ),
          // 임시: 로그인 흐름 테스트용 로그아웃. "..." 메뉴 본 기능은 아직 범위 밖.
          headerRight: () => (
            <Pressable hitSlop={10} onPress={logout}>
              <Ionicons name="ellipsis-horizontal" size={22} color="#6f735b" />
            </Pressable>
          ),
          headerStyle: { backgroundColor: "#faf7f0" },
          headerShadowVisible: false,
          headerTintColor: "#394437",
        }}
      />

      <FlatList
        inverted
        data={messages}
        keyExtractor={(m) => m.id}
        contentContainerStyle={styles.list}
        keyboardDismissMode="interactive"
        // inverted라서 header는 화면 아래쪽(최신), footer는 위쪽(대화 시작)에 그려진다.
        ListHeaderComponent={
          <>
            {isSending && (
              <View style={styles.rowLeft}>
                <View style={[styles.bubble, styles.bubbleBarnabas]}>
                  <TypingDots />
                </View>
              </View>
            )}
            {messages[0]?.fromBarnabas ? (
              <View style={styles.quickReplies}>
                {QUICK_REPLIES.map((reply) => (
                  <Pressable
                    key={reply}
                    onPress={() => sendText(reply)}
                    style={styles.quickReplyChip}
                  >
                    <Text style={styles.quickReplyText}>{reply}</Text>
                  </Pressable>
                ))}
              </View>
            ) : null}
          </>
        }
        ListFooterComponent={
          <View style={styles.dayChip}>
            <Text style={styles.dayChipText}>오늘</Text>
          </View>
        }
        renderItem={({ item }) => {
          if (item.notice) {
            return (
              <View style={styles.noticeBox}>
                <Text style={styles.noticeText}>{item.text}</Text>
              </View>
            );
          }
          return (
            <View style={item.fromBarnabas ? styles.rowLeft : styles.rowRight}>
              <View
                style={[
                  styles.bubble,
                  item.fromBarnabas ? styles.bubbleBarnabas : styles.bubbleMe,
                ]}
              >
                {item.fromBarnabas ? (
                  <Markdown style={markdownStyles} rules={markdownRules}>
                    {item.text}
                  </Markdown>
                ) : (
                  <Text selectable style={styles.textMe}>
                    {item.text}
                  </Text>
                )}
                {item.passages?.map((p) => (
                  <VerseQuote key={p.reference} passage={p} />
                ))}
              </View>
            </View>
          );
        }}
      />

      <View style={styles.inputBar}>
        <View style={styles.inputContainer}>
          {/* 첨부는 MVP 범위 밖이라 아직 동작하지 않는다 (시안 자리만 유지). */}
          <Pressable disabled hitSlop={8} style={styles.attachButton}>
            <Ionicons name="add" size={24} color="#c2beae" />
          </Pressable>
          <TextInput
            style={styles.input}
            value={draft}
            onChangeText={setDraft}
            placeholder="바나바에게 이야기하기"
            placeholderTextColor="#a8a495"
            multiline
          />
          <Pressable
            onPress={send}
            disabled={!canSend}
            style={[styles.sendButton, !canSend && styles.sendButtonDisabled]}
          >
            <Ionicons name="send" size={18} color="#ffffff" />
          </Pressable>
        </View>
      </View>
    </Animated.View>
  );
}

/** 바나바 응답은 마크다운으로 올 수 있어 렌더링한다. 말풍선 텍스트 스타일과 맞춘다. */
const markdownStyles = {
  body: { fontSize: 16, lineHeight: 25, color: "#2f3328" },
  heading1: { fontSize: 19, fontWeight: "700" as const, marginTop: 4, marginBottom: 4 },
  heading2: { fontSize: 18, fontWeight: "700" as const, marginTop: 4, marginBottom: 4 },
  heading3: { fontSize: 17, fontWeight: "700" as const, marginTop: 4, marginBottom: 4 },
  strong: { fontWeight: "700" as const },
  bullet_list: { marginVertical: 4 },
  ordered_list: { marginVertical: 4 },
  hr: { backgroundColor: "#e0dbcc", height: 1, marginVertical: 8 },
};

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#faf7f0",
  },
  headerTitle: {
    fontSize: 19,
    fontWeight: "700",
    color: "#2f3328",
  },
  headerSubtitle: {
    marginTop: 2,
    fontSize: 12,
    color: "#8a8b78",
  },
  list: {
    paddingHorizontal: 16,
    paddingVertical: 16,
  },
  dayChip: {
    alignSelf: "center",
    marginVertical: 12,
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 14,
    backgroundColor: "#ece7db",
  },
  dayChipText: {
    fontSize: 12,
    fontWeight: "600",
    color: "#6f735b",
  },
  noticeBox: {
    marginVertical: 5,
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: "#e6e0d2",
  },
  noticeText: {
    fontSize: 14,
    lineHeight: 22,
    color: "#6f735b",
  },
  rowLeft: {
    alignItems: "flex-start",
    marginVertical: 5,
  },
  rowRight: {
    alignItems: "flex-end",
    marginVertical: 5,
  },
  bubble: {
    maxWidth: "84%",
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 18,
  },
  bubbleBarnabas: {
    backgroundColor: "#ffffff",
    borderTopLeftRadius: 6,
    // 흰 말풍선과 크림 배경은 명도 차가 작아 그림자가 없으면 경계가 뭉개진다.
    shadowColor: "#7a6a4a",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 6,
    elevation: 2,
  },
  bubbleMe: {
    backgroundColor: "#394437",
    borderTopRightRadius: 6,
  },
  textMe: {
    fontSize: 16,
    lineHeight: 25,
    color: "#ffffff",
  },
  typingRow: {
    flexDirection: "row",
    gap: 4,
    paddingVertical: 4,
  },
  typingDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: "#8a8b78",
  },
  quickReplies: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginTop: 10,
  },
  quickReplyChip: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: "#e0dbcc",
    backgroundColor: "#ffffff",
  },
  quickReplyText: {
    fontSize: 14,
    fontWeight: "600",
    color: "#4a4436",
  },
  inputBar: {
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 10,
  },
  inputContainer: {
    flexDirection: "row",
    alignItems: "flex-end",
    paddingLeft: 8,
    paddingRight: 8,
    paddingVertical: 8,
    borderRadius: 28,
    backgroundColor: "#ffffff",
  },
  attachButton: {
    width: 36,
    height: 40,
    alignItems: "center",
    justifyContent: "center",
  },
  input: {
    flex: 1,
    maxHeight: 120,
    minHeight: 40,
    paddingHorizontal: 6,
    paddingVertical: 10,
    fontSize: 16,
    color: "#2f3328",
  },
  sendButton: {
    marginLeft: 6,
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#394437",
  },
  sendButtonDisabled: {
    backgroundColor: "#c5c8bc",
  },
});
