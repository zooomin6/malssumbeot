import { useAuth } from "@/contexts/AuthContext";
import { useState } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";

export default function LoginScreen() {
  const { loginWithDevToken } = useAuth();
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  // 실제 구글/카카오 로그인은 개발 빌드 전환 후 네이티브 SDK로 대체 예정(D-028 참고).
  // 지금은 dev 프로파일 전용 토큰으로 화면·흐름만 검증한다.
  // 토큰이 설정되면 Stack.Protected가 자동으로 화면 그룹을 전환하므로 별도 이동 호출은 없다.
  const handleDevLogin = async () => {
    setIsLoggingIn(true);
    try {
      await loginWithDevToken();
    } catch (err) {
      console.error("로그인 실패", err);
    } finally {
      setIsLoggingIn(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>엠마오</Text>
      <Text style={styles.subtitle}>바나바와 함께 말씀 곁을 걷습니다</Text>

      <Pressable style={styles.devButton} onPress={handleDevLogin} disabled={isLoggingIn}>
        {isLoggingIn ? (
          <ActivityIndicator color="#ffffff" />
        ) : (
          <Text style={styles.devButtonText}>개발용으로 시작하기</Text>
        )}
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#faf7f0",
    paddingHorizontal: 24,
  },
  title: {
    fontSize: 32,
    fontWeight: "700",
    color: "#2f3328",
  },
  subtitle: {
    marginTop: 8,
    fontSize: 14,
    color: "#8a8b78",
  },
  devButton: {
    marginTop: 48,
    paddingHorizontal: 32,
    paddingVertical: 14,
    borderRadius: 24,
    backgroundColor: "#394437",
  },
  devButtonText: {
    color: "#ffffff",
    fontSize: 16,
    fontWeight: "600",
  },
});
