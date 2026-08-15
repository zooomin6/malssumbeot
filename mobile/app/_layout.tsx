import { Stack } from "expo-router";
import { KeyboardProvider } from "react-native-keyboard-controller";

export default function RootLayout() {
  return (
    <KeyboardProvider>
      <Stack>
        {/* 진입 시트는 앞 화면 위에 겹쳐 올라오는 바텀시트라 투명 모달로 띄운다. */}
        <Stack.Screen
          name="chat-entry"
          options={{
            presentation: "transparentModal",
            animation: "slide_from_bottom",
            headerShown: false,
          }}
        />
      </Stack>
    </KeyboardProvider>
  );
}
