import { AuthProvider, useAuth } from "@/contexts/AuthContext";
import { Stack } from "expo-router";
import { ActivityIndicator, View } from "react-native";
import { KeyboardProvider } from "react-native-keyboard-controller";

/** 인증 상태에 따라 화면 그룹 자체를 다르게 구성한다(Stack.Protected, Expo Router 공식 인증 가드 패턴). */
function RootNavigator() {
  const { token, isLoading } = useAuth();

  if (isLoading) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator />
      </View>
    );
  }

  return (
    <Stack>
      <Stack.Protected guard={!!token}>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="chat" />
        {/* 진입 시트/입력 시트는 앞 화면 위에 겹쳐 올라오는 바텀시트라 투명 모달로 띄운다. */}
        <Stack.Screen
          name="chat-entry"
          options={{
            presentation: "transparentModal",
            animation: "slide_from_bottom",
            headerShown: false,
          }}
        />
        <Stack.Screen
          name="mind-record-new"
          options={{
            presentation: "transparentModal",
            animation: "slide_from_bottom",
            headerShown: false,
          }}
        />
        <Stack.Screen
          name="prayer-request-new"
          options={{
            presentation: "transparentModal",
            animation: "slide_from_bottom",
            headerShown: false,
          }}
        />
      </Stack.Protected>

      <Stack.Protected guard={!token}>
        <Stack.Screen name="login" options={{ headerShown: false }} />
      </Stack.Protected>
    </Stack>
  );
}

export default function RootLayout() {
  return (
    <AuthProvider>
      <KeyboardProvider>
        <RootNavigator />
      </KeyboardProvider>
    </AuthProvider>
  );
}

const styles = {
  loading: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },
} as const;
