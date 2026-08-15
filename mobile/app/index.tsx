import { Link } from "expo-router";
import { StyleSheet, Text, View } from "react-native";

export default function Index() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>엠마오</Text>
      <Text style={styles.subtitle}>스캐폴딩 확인용 화면</Text>
      <Link href="/chat-entry" style={styles.link}>
        대화 시작 시트 열기
      </Link>
      <Link href="/chat" style={styles.link}>
        채팅 화면 열기
      </Link>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },
  title: {
    fontSize: 32,
    fontWeight: "600",
  },
  subtitle: {
    marginTop: 8,
    fontSize: 14,
    opacity: 0.6,
  },
  link: {
    marginTop: 24,
    fontSize: 16,
    color: "#2563eb",
  },
});
