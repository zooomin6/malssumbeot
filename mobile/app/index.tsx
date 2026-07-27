import { StyleSheet, Text, View } from "react-native";

export default function Index() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>엠마오</Text>
      <Text style={styles.subtitle}>스캐폴딩 확인용 화면</Text>
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
});
