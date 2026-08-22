import { Redirect } from "expo-router";

/** 콘텐츠 없는 탭 — 누르면 곧바로 대화 진입 시트로 보낸다(기존 app/index.tsx 방식 재사용). */
export default function BarnabasTab() {
  return <Redirect href="/chat-entry" />;
}
