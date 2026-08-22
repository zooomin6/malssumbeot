import { colors } from "@/constants/colors";
import { useAuth } from "@/contexts/AuthContext";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

/** "나" 탭. chat.tsx 헤더에 있던 임시 로그아웃을 여기로 옮겼다. */
export default function ProfileTab() {
  const { logout } = useAuth();
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.screen, { paddingTop: insets.top + 24 }]}>
      <Text style={styles.title}>나</Text>
      <Pressable onPress={logout} style={styles.logoutButton}>
        <Text style={styles.logoutText}>로그아웃</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
    paddingHorizontal: 20,
  },
  title: {
    fontSize: 22,
    fontWeight: "700",
    color: colors.text,
    marginBottom: 24,
  },
  logoutButton: {
    alignSelf: "flex-start",
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.card,
  },
  logoutText: {
    fontSize: 15,
    fontWeight: "600",
    color: colors.textSecondary,
  },
});
