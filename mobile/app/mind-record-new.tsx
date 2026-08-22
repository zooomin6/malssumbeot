import { API_BASE_URL } from "@/constants/api";
import { colors } from "@/constants/colors";
import { useAuth } from "@/contexts/AuthContext";
import { router } from "expo-router";
import { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

/** 홈 화면 "마음 기록" 카드에서 열리는 자유 입력 시트. chat-entry.tsx와 같은 바텀시트 구조. */
export default function MindRecordNewScreen() {
  const { token } = useAuth();
  const insets = useSafeAreaInsets();
  const [content, setContent] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const canSave = content.trim().length > 0 && !isSaving;

  const save = async () => {
    if (!canSave || !token) return;
    setIsSaving(true);
    try {
      await fetch(`${API_BASE_URL}/api/mind-records`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify({ mood: null, content: content.trim() }),
      });
      router.back();
    } catch (err) {
      console.error("마음 기록 저장 실패", err);
      setIsSaving(false);
    }
  };

  return (
    <View style={styles.overlay}>
      <Pressable style={styles.backdrop} onPress={() => router.back()} />

      <View style={[styles.sheet, { paddingBottom: insets.bottom + 20 }]}>
        <View style={styles.handle} />
        <Text style={styles.title}>지금의 마음을 남겨보세요</Text>
        <TextInput
          style={styles.input}
          value={content}
          onChangeText={setContent}
          placeholder="오늘 마음이 어땠는지 짧게 적어보세요"
          placeholderTextColor={colors.textMuted}
          multiline
          autoFocus
        />
        <Pressable onPress={save} disabled={!canSave} style={[styles.cta, !canSave && styles.ctaDisabled]}>
          <Text style={styles.ctaLabel}>저장하기</Text>
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
  title: {
    marginTop: 20,
    fontSize: 20,
    fontWeight: "700",
    color: colors.text,
  },
  input: {
    marginTop: 18,
    minHeight: 100,
    maxHeight: 200,
    padding: 16,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.card,
    fontSize: 15,
    lineHeight: 22,
    color: colors.text,
  },
  cta: {
    marginTop: 16,
    height: 52,
    borderRadius: 24,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.buttonDark,
  },
  ctaDisabled: {
    backgroundColor: "#c5c8bc",
  },
  ctaLabel: {
    fontSize: 16,
    fontWeight: "700",
    color: "#ffffff",
  },
});
