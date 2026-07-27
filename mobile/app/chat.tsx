import { Chat, IMessage } from "@kesha-antonov/react-native-chat";
import { useCallback, useState } from "react";

/** 챗봇 = 바나바 (D-021). 사용자는 로그인 붙기 전까지 임시 ID. */
const BARNABAS = { _id: "barnabas", name: "바나바" };
const ME = { _id: "me" };

export default function ChatScreen() {
  const [messages, setMessages] = useState<IMessage[]>([
    {
      _id: 1,
      text: "안녕하세요, 바나바입니다. 오늘 마음은 어떠신가요?",
      createdAt: new Date(),
      user: BARNABAS,
    },
  ]);

  const onSend = useCallback((sent: IMessage[]) => {
    setMessages((previous) => Chat.append(previous, sent));
  }, []);

  return <Chat messages={messages} user={ME} onSend={onSend} />;
}
