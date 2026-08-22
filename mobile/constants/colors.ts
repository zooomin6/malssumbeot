/**
 * 디자인 v1(docs/design, D-036) 팔레트. `VerseQuote.tsx`·`chat.tsx`는 만들어질 때부터 색을
 * 직접 하드코딩해왔고 이번 작업 대상이 아니라 그대로 둔다 — 이 파일은 이번에 새로 생기는
 * 홈/보관함/나 화면들이 공유해서 쓴다.
 */
export const colors = {
  background: "#faf7f0",
  card: "#ffffff",
  accent: "#d99a5e",
  accentDark: "#b1793d",
  text: "#2f3328",
  textSecondary: "#4a4436",
  textMuted: "#8a8b78",
  textMutedAlt: "#6f735b",
  border: "#e6e0d2",
  buttonDark: "#394437",
} as const;
