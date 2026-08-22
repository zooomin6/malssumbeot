import { API_BASE_URL } from "@/constants/api";
import { colors } from "@/constants/colors";
import { useAuth } from "@/contexts/AuthContext";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

/** 기분 칩. 누르면 화면에 선택 표시가 되는 동시에 마음기록에 저장된다(민규 결정). */
const MOODS = ["조금 지쳤어요", "감사한 마음", "길을 찾는 중"];

/** 오늘의 말씀은 이번 마일스톤에서는 고정 문구. 성경 DB 복구 완료 후 실제 API로 교체 예정. */
const VERSE_OF_DAY = {
  text: "길에서 우리에게 말씀하시고 성경을 풀어 주실 때에 마음이 뜨겁지 아니하더냐",
  reference: "누가복음 24:32",
};

export default function HomeTab() {
  const { token } = useAuth();
  const insets = useSafeAreaInsets();
  const [selectedMood, setSelectedMood] = useState<string | null>(null);

  const selectMood = async (mood: string) => {
    setSelectedMood(mood);
    if (!token) return;
    try {
      await fetch(`${API_BASE_URL}/api/mind-records`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify({ mood, content: mood }),
      });
    } catch (err) {
      console.error("마음 기록 저장 실패", err);
    }
  };

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 16 }]}
    >
      <View style={styles.header}>
        <View style={styles.brand}>
          <View style={styles.emblem} />
          <Text style={styles.brandText}>엠마오</Text>
        </View>
        {/* 알림 기능은 범위 밖이라 자리만 유지한다(chat.tsx의 첨부 버튼과 같은 방식). */}
        <Pressable disabled hitSlop={8} style={styles.bellButton}>
          <Ionicons name="notifications-outline" size={20} color={colors.textMuted} />
        </Pressable>
      </View>

      <Text style={styles.greeting}>안녕하세요, 오늘도 잘 오셨어요</Text>
      <Text style={styles.question}>오늘은 어떤 마음으로{"\n"}찾아오셨나요?</Text>
      <Text style={styles.subtitle}>천천히 들려주세요. 바나바가 말씀 곁에서 함께 걸을게요.</Text>

      <View style={styles.moodRow}>
        {MOODS.map((mood) => (
          <Pressable
            key={mood}
            onPress={() => selectMood(mood)}
            style={[styles.moodChip, selectedMood === mood && styles.moodChipSelected]}
          >
            <Text style={[styles.moodChipText, selectedMood === mood && styles.moodChipTextSelected]}>
              {mood}
            </Text>
          </Pressable>
        ))}
      </View>

      <View style={styles.verseCard}>
        <Text style={styles.verseLabel}>오늘의 말씀</Text>
        <Text style={styles.verseText}>“{VERSE_OF_DAY.text}”</Text>
        <Text style={styles.verseReference}>{VERSE_OF_DAY.reference}</Text>
      </View>

      <View style={styles.shortcutRow}>
        <Pressable onPress={() => router.push("/mind-record-new")} style={styles.shortcutCard}>
          <View style={styles.shortcutHeader}>
            <Text style={styles.shortcutTitle}>마음 기록</Text>
            <Ionicons name="sparkles-outline" size={18} color={colors.accentDark} />
          </View>
          <Text style={styles.shortcutDescription}>지금의 마음을 짧게 남겨보세요</Text>
        </Pressable>

        <Pressable onPress={() => router.push("/prayer-request-new")} style={styles.shortcutCard}>
          <View style={styles.shortcutHeader}>
            <Text style={styles.shortcutTitle}>기도 제목</Text>
            <Ionicons name="add" size={18} color={colors.accentDark} />
          </View>
          <Text style={styles.shortcutDescription}>기억하고 싶은 기도를 모아봐요</Text>
        </Pressable>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    paddingHorizontal: 20,
    paddingBottom: 40,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  brand: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  emblem: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: colors.accent,
  },
  brandText: {
    fontSize: 17,
    fontWeight: "700",
    color: colors.text,
  },
  bellButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.card,
  },
  greeting: {
    marginTop: 28,
    fontSize: 14,
    fontWeight: "600",
    color: colors.textMutedAlt,
  },
  question: {
    marginTop: 6,
    fontSize: 25,
    fontWeight: "700",
    lineHeight: 32,
    color: colors.text,
  },
  subtitle: {
    marginTop: 10,
    fontSize: 14,
    lineHeight: 21,
    color: colors.textMuted,
  },
  moodRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginTop: 18,
  },
  moodChip: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.card,
  },
  moodChipSelected: {
    borderColor: colors.accent,
    backgroundColor: "#f8ecd9",
  },
  moodChipText: {
    fontSize: 13,
    fontWeight: "600",
    color: colors.textSecondary,
  },
  moodChipTextSelected: {
    color: colors.accentDark,
  },
  verseCard: {
    marginTop: 20,
    padding: 20,
    borderRadius: 18,
    backgroundColor: "#f2ebdd",
  },
  verseLabel: {
    fontSize: 13,
    fontWeight: "700",
    color: colors.accentDark,
  },
  verseText: {
    marginTop: 14,
    fontSize: 17,
    lineHeight: 26,
    fontWeight: "600",
    color: colors.text,
  },
  verseReference: {
    marginTop: 12,
    fontSize: 13,
    color: colors.textMutedAlt,
  },
  shortcutRow: {
    flexDirection: "row",
    gap: 12,
    marginTop: 16,
  },
  shortcutCard: {
    flex: 1,
    padding: 16,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.card,
  },
  shortcutHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  shortcutTitle: {
    fontSize: 15,
    fontWeight: "700",
    color: colors.text,
  },
  shortcutDescription: {
    marginTop: 6,
    fontSize: 12,
    lineHeight: 18,
    color: colors.textMuted,
  },
});
