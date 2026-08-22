import { colors } from "@/constants/colors";
import { Ionicons } from "@expo/vector-icons";
import { Tabs } from "expo-router";

/** 하단 탭 5개(홈/말씀/바나바/보관함/나, D-036 시안). 바나바 탭은 콘텐츠 없이 대화 진입
 * 시트로 즉시 넘어간다(app/(tabs)/barnabas.tsx). */
export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.accentDark,
        tabBarInactiveTintColor: colors.textMuted,
        tabBarStyle: { backgroundColor: colors.card, borderTopColor: colors.border },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: "홈",
          tabBarIcon: ({ color, size }) => <Ionicons name="home-outline" size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="bible"
        options={{
          title: "말씀",
          tabBarIcon: ({ color, size }) => <Ionicons name="book-outline" size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="barnabas"
        options={{
          title: "바나바",
          // 브랜드 마크 PNG가 아직 없어(PROGRESS.md) 강조색 아이콘으로 대신한다.
          tabBarIcon: ({ size }) => <Ionicons name="sparkles" size={size} color={colors.accent} />,
        }}
      />
      <Tabs.Screen
        name="archive"
        options={{
          title: "보관함",
          tabBarIcon: ({ color, size }) => <Ionicons name="bookmark-outline" size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: "나",
          tabBarIcon: ({ color, size }) => <Ionicons name="person-outline" size={size} color={color} />,
        }}
      />
    </Tabs>
  );
}
