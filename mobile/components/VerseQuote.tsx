import { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

/**
 * 서버 `ChatResponse.Passage`와 같은 모양.
 * 여기 담기는 본문은 DB 검증을 통과한 개역한글 원문뿐이다 (D-003, D-025).
 */
export type Passage = {
  reference: string;
  bookName: string;
  chapter: number;
  verseStart: number;
  verseEnd: number;
  verses: { verse: number; text: string }[];
};

/** 접힘 상태에서 보여줄 본문 줄 수. 넘치면 RN이 말줄임표를 붙인다. */
const COLLAPSED_LINES = 2;

/**
 * 구절 인용 카드. 카드와 출처는 항상 보이고(디자인 원칙 01 "말씀은 분명하게"),
 * 본문은 두 줄로 잘라 보여준다. 탭하면 전문과 성명표시권 표기가 펼쳐진다.
 */
export function VerseQuote({ passage }: { passage: Passage }) {
  const [open, setOpen] = useState(false);
  const body = passage.verses.map((v) => v.text).join(" ");

  return (
    <Pressable onPress={() => setOpen(!open)} style={styles.card}>
      <Text style={styles.body} numberOfLines={open ? undefined : COLLAPSED_LINES}>
        “{body}”
      </Text>
      <View style={styles.footer}>
        <Text style={styles.reference}>{passage.reference}</Text>
        {/* 개역한글은 재산권은 만료됐지만 성명표시권은 남아 있어 표기가 필요하다 (D-016) */}
        {open && (
          <Text style={styles.attribution}>성경전서 개역한글판, 대한성서공회</Text>
        )}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    marginTop: 10,
    paddingVertical: 12,
    paddingHorizontal: 14,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: "#d99a5e",
    backgroundColor: "#f2ebdd",
  },
  body: {
    fontSize: 15,
    lineHeight: 24,
    color: "#4a4436",
  },
  footer: {
    marginTop: 8,
  },
  reference: {
    fontSize: 13,
    fontWeight: "700",
    color: "#6f735b",
  },
  attribution: {
    marginTop: 6,
    fontSize: 11,
    color: "#9a9583",
  },
});
