import { colors } from "@/constants/colors";
import { StyleSheet, Text, View } from "react-native";

/** 말씀 탭 스텁. 성경 검색 등 실제 기능은 다음 세션 범위. */
export default function BibleTab() {
  return (
    <View style={styles.screen}>
      <Text style={styles.text}>준비 중입니다</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: "center",
    justifyContent: "center",
  },
  text: {
    fontSize: 15,
    color: colors.textMuted,
  },
});
