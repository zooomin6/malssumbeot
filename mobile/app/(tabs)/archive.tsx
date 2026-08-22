import { API_BASE_URL } from "@/constants/api";
import { colors } from "@/constants/colors";
import { useAuth } from "@/contexts/AuthContext";
import { Ionicons } from "@expo/vector-icons";
import { useFocusEffect } from "@react-navigation/native";
import { useCallback, useState } from "react";
import { FlatList, Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

type ArchivedVerse = { reference: string; text: string };
type ArchiveItem = {
  id: number;
  userMessage: string | null;
  barnabasReply: string;
  verses: ArchivedVerse[];
  createdAt: string;
};

/** 보관함 탭. 채팅에서 북마크한 항목을 조회·삭제한다. 탭에 다시 들어올 때마다 새로고침한다. */
export default function ArchiveTab() {
  const { token } = useAuth();
  const insets = useSafeAreaInsets();
  const [items, setItems] = useState<ArchiveItem[]>([]);

  const load = useCallback(async () => {
    if (!token) return;
    try {
      const res = await fetch(`${API_BASE_URL}/api/archive-items`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setItems(await res.json());
    } catch (err) {
      console.error("보관함 조회 실패", err);
    }
  }, [token]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const remove = async (id: number) => {
    if (!token) return;
    setItems((prev) => prev.filter((item) => item.id !== id));
    try {
      await fetch(`${API_BASE_URL}/api/archive-items/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });
    } catch (err) {
      console.error("보관함 삭제 실패", err);
    }
  };

  return (
    <FlatList
      style={styles.screen}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 16 }]}
      data={items}
      keyExtractor={(item) => String(item.id)}
      ListHeaderComponent={<Text style={styles.title}>보관함</Text>}
      ListEmptyComponent={<Text style={styles.empty}>아직 저장한 대화가 없어요</Text>}
      renderItem={({ item }) => (
        <View style={styles.card}>
          {item.userMessage && <Text style={styles.userMessage}>{item.userMessage}</Text>}
          <Text style={styles.reply}>{item.barnabasReply}</Text>
          {item.verses.length > 0 && (
            <View style={styles.verseRow}>
              {item.verses.map((v) => (
                <View key={v.reference} style={styles.verseChip}>
                  <Text style={styles.verseChipText}>{v.reference}</Text>
                </View>
              ))}
            </View>
          )}
          <Pressable onPress={() => remove(item.id)} style={styles.deleteButton} hitSlop={8}>
            <Ionicons name="trash-outline" size={16} color={colors.textMuted} />
          </Pressable>
        </View>
      )}
    />
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
  title: {
    fontSize: 22,
    fontWeight: "700",
    color: colors.text,
    marginBottom: 16,
  },
  empty: {
    marginTop: 40,
    textAlign: "center",
    fontSize: 14,
    color: colors.textMuted,
  },
  card: {
    marginBottom: 12,
    padding: 16,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.card,
  },
  userMessage: {
    fontSize: 13,
    color: colors.textMutedAlt,
    marginBottom: 6,
  },
  reply: {
    fontSize: 15,
    lineHeight: 22,
    color: colors.text,
  },
  verseRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 6,
    marginTop: 10,
  },
  verseChip: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
    backgroundColor: "#f2ebdd",
  },
  verseChipText: {
    fontSize: 12,
    fontWeight: "600",
    color: colors.accentDark,
  },
  deleteButton: {
    alignSelf: "flex-end",
    marginTop: 8,
  },
});
