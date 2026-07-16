package com.malssumbeot.api;

import com.malssumbeot.orchestrator.ChatOrchestrator;
import com.malssumbeot.orchestrator.ChatReply;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 채팅 REST 엔드포인트 — 모든 채팅 트래픽의 단일 진입점.
 *
 * 위기 우회 불가(D-004)는 이 단일 경로로 보장한다: {@link ChatOrchestrator#handle}이 첫 단계로
 * 위기 감지를 수행하므로, 채팅이 여기만 거치는 한 위기 분기는 건너뛸 수 없다. 엔드포인트가 여러 개로
 * 늘어나면 그때 HandlerInterceptor로 방어선을 한 겹 더 올린다 (ROADMAP Phase 1 후속).
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatOrchestrator orchestrator;

    public ChatController(ChatOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        ChatReply reply = orchestrator.handle(request.sessionId(), request.message());
        return ChatResponse.from(reply);
    }
}
