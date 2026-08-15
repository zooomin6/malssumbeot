import { Passage, VerseQuote } from "@/components/VerseQuote";
import { Ionicons } from "@expo/vector-icons";
import { Stack } from "expo-router";
import { useState } from "react";
import {
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { useReanimatedKeyboardAnimation } from "react-native-keyboard-controller";
import Animated, { useAnimatedStyle } from "react-native-reanimated";
import { useSafeAreaInsets } from "react-native-safe-area-context";

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

/**
 * 서버 연결 전 목업. 실제로는 `POST /api/chat`의 응답이 그대로 들어온다.
 * 목록을 inverted로 그리므로 최신 메시지가 배열 앞에 온다.
 */
const INITIAL_MESSAGES: Message[] = [
  {
    id: "4",
    text: "지금 가장 마음을 재촉하는 한 가지는 무엇인가요? 함께 천천히 들여다볼까요?",
    fromBarnabas: true,
  },
  {
    id: "3",
    text: "잘해내고 싶은 마음이 큰 만큼, 마음이 먼저 달려가고 있나 봐요. 잠시 멈춰도 괜찮아요.",
    fromBarnabas: true,
    passages: [
      {
        reference: "빌립보서 4:6-7",
        bookName: "빌립보서",
        chapter: 4,
        verseStart: 6,
        verseEnd: 7,
        // 본문은 비워둔다. 개역한글 원문은 서버가 DB에서 조회해 내려주는 것만 쓴다 (절대원칙 2).
        verses: [
          {
            verse: 6,
            text: "서버 연결 시 이 자리에 DB에서 조회한 개역한글 원문이 표시됩니다. 지금은 화면 확인용 자리표시자이므로 실제 성경 본문이 아닙니다.",
          },
        ],
      },
    ],
  },
  {
    id: "2",
    text: "요즘 마음이 자꾸 조급해져요. 잘하고 있는 건지 모르겠어요.",
    fromBarnabas: false,
  },
  {
    id: "1",
    text: "바나바는 정답을 대신 내리기보다, 성경 말씀을 바탕으로 마음을 천천히 살피도록 도와드려요.",
    fromBarnabas: true,
    notice: true,
  },
];

export default function ChatScreen() {
  const [messages, setMessages] = useState<Message[]>(INITIAL_MESSAGES);
  const [draft, setDraft] = useState("");
  const insets = useSafeAreaInsets();
  const canSend = draft.trim().length > 0;

  // KeyboardProvider가 창 축소를 가져가므로 아래 여백을 직접 만들어 입력창을 밀어 올린다.
  // height는 키보드가 열릴수록 음수로 커지고, 매 프레임 갱신돼 키보드를 그대로 따라간다.
  // 닫힌 동안에는 하단 제스처바 여백이 대신 자리를 지킨다.
  const { height: keyboardHeight } = useReanimatedKeyboardAnimation();
  const bottomInset = useAnimatedStyle(
    () => ({ paddingBottom: Math.max(-keyboardHeight.value, insets.bottom) }),
    [insets.bottom],
  );

  const sendText = (text: string) => {
    const trimmed = text.trim();
    if (!trimmed) return;
    setMessages((prev) => [
      { id: `${Date.now()}-${prev.length}`, text: trimmed, fromBarnabas: false },
      ...prev,
    ]);
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
          headerRight: () => (
            <Pressable hitSlop={10}>
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
          messages[0]?.fromBarnabas ? (
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
          ) : null
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
                <Text
                  style={item.fromBarnabas ? styles.textBarnabas : styles.textMe}
                >
                  {item.text}
                </Text>
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
  textBarnabas: {
    fontSize: 16,
    lineHeight: 25,
    color: "#2f3328",
  },
  textMe: {
    fontSize: 16,
    lineHeight: 25,
    color: "#ffffff",
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
