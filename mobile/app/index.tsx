import { Redirect } from "expo-router";

/** 인증된 사용자의 앵커 화면. 대화 진입 시트로 바로 보낸다. */
export default function Index() {
  return <Redirect href="/chat-entry" />;
}
