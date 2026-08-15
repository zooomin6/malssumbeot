import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

type Option = {
  icon: React.ComponentProps<typeof Ionicons>["name"];
  title: string;
  description: string;
};

/**
 * 대화 시작 전 마음을 고르는 선택지.
 * 지금은 셋 다 채팅 화면으로만 넘어간다 — 이 선택을 서버 의도 분류에 넘길지는 미정.
 */
const OPTIONS: Option[] = [
  {
    icon: "chatbubble-outline",
    title: "마음을 나누고 싶어요",
    description: "답답함, 관계, 진로, 신앙 이야기",
  },
  {
    icon: "book-outline",
    title: "말씀을 찾고 싶어요",
    description: "상황에 맞는 성경 말씀과 묵상",
  },
  {
    icon: "heart-outline",
    title: "기도문이 필요해요",
    description: "지금의 상황을 담은 짧은 기도",
  },
];

export default function ChatEntryScreen() {
  const insets = useSafeAreaInsets();
  const openChat = () => router.replace("/chat");

  return (
    <View style={styles.overlay}>
      <Pressable style={styles.backdrop} onPress={() => router.back()} />

      <View style={[styles.sheet, { paddingBottom: insets.bottom + 20 }]}>
        <View style={styles.handle} />

        {/* 바나바 엠블럼. 브랜드 마크(길의 십자가) PNG가 아직 없어 원형만 그린다. */}
        <View style={styles.emblemOuterGlow}>
          <View style={styles.emblemInnerGlow}>
            <View style={styles.emblem} />
          </View>
        </View>

        <Text style={styles.title}>바나바와 이야기할까요?</Text>
        <Text style={styles.subtitle}>
          지금 떠오르는 마음을 고르거나{"\n"}그냥 편하게 이야기를 시작해도 좋아요.
        </Text>

        <View style={styles.options}>
          {OPTIONS.map((option) => (
            <Pressable
              key={option.title}
              onPress={openChat}
              style={styles.optionCard}
            >
              <View style={styles.optionIcon}>
                <Ionicons name={option.icon} size={22} color="#b1793d" />
              </View>
              <View style={styles.optionTexts}>
                <Text style={styles.optionTitle}>{option.title}</Text>
                <Text style={styles.optionDescription}>{option.description}</Text>
              </View>
            </Pressable>
          ))}
        </View>

        <Pressable onPress={openChat} style={styles.cta}>
          <Text style={styles.ctaLabel}>직접 이야기 시작하기</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    justifyContent: "flex-end",
  },
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(47, 51, 40, 0.28)",
  },
  sheet: {
    paddingTop: 14,
    paddingHorizontal: 20,
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: "#fdfbf7",
  },
  handle: {
    alignSelf: "center",
    width: 44,
    height: 5,
    borderRadius: 3,
    backgroundColor: "#dcd6c8",
  },
  emblemOuterGlow: {
    alignSelf: "center",
    marginTop: 28,
    padding: 18,
    borderRadius: 999,
    backgroundColor: "rgba(217, 154, 94, 0.07)",
  },
  emblemInnerGlow: {
    padding: 12,
    borderRadius: 999,
    backgroundColor: "rgba(217, 154, 94, 0.12)",
  },
  emblem: {
    width: 104,
    height: 104,
    borderRadius: 52,
    backgroundColor: "#d99a5e",
  },
  title: {
    marginTop: 26,
    fontSize: 25,
    fontWeight: "700",
    textAlign: "center",
    color: "#2f3328",
  },
  subtitle: {
    marginTop: 10,
    fontSize: 15,
    lineHeight: 23,
    textAlign: "center",
    color: "#8a8b78",
  },
  options: {
    marginTop: 26,
    gap: 10,
  },
  optionCard: {
    flexDirection: "row",
    alignItems: "center",
    padding: 14,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "#f0ebe0",
    backgroundColor: "#ffffff",
  },
  optionIcon: {
    width: 46,
    height: 46,
    borderRadius: 14,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#f2ebdd",
  },
  optionTexts: {
    flex: 1,
    marginLeft: 14,
  },
  optionTitle: {
    fontSize: 16,
    fontWeight: "700",
    color: "#2f3328",
  },
  optionDescription: {
    marginTop: 3,
    fontSize: 13,
    color: "#8a8b78",
  },
  cta: {
    marginTop: 20,
    height: 56,
    borderRadius: 26,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#394437",
  },
  ctaLabel: {
    fontSize: 17,
    fontWeight: "700",
    color: "#ffffff",
  },
});
