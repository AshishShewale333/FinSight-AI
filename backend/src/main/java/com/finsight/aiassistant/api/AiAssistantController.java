package com.finsight.aiassistant.api;

import com.finsight.aiassistant.application.AiAssistantService;
import com.finsight.aiassistant.application.ChatSessionService;
import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/ai")
public class AiAssistantController {
    private final AiAssistantService ai;
    private final ChatSessionService sessions;
    public AiAssistantController(AiAssistantService ai,ChatSessionService sessions) { this.ai=ai; this.sessions=sessions; }

    @PostMapping("/chat/sessions")
    public ApiResponse<ChatSessionResponse> createSession(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateChatSessionRequest body,HttpServletRequest request) {
        return ApiResponse.success("Chat session created",sessions.create(jwt.getSubject(),body),cid(request));
    }
    @GetMapping("/chat/sessions")
    public ApiResponse<Page<ChatSessionResponse>> listSessions(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,HttpServletRequest request) {
        return ApiResponse.success("Chat sessions retrieved",sessions.list(jwt.getSubject(),page,size),cid(request));
    }
    @GetMapping("/chat/sessions/{sessionId}/messages")
    public ApiResponse<Page<ChatMessageResponse>> messages(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String sessionId,@RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="50") @Min(1) @Max(100) int size,HttpServletRequest request) {
        return ApiResponse.success("Chat messages retrieved",sessions.messages(jwt.getSubject(),sessionId,page,size),cid(request));
    }
    @PostMapping("/chat")
    public ApiResponse<ChatAnswerResponse> chat(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChatRequest body,HttpServletRequest request) {
        return ApiResponse.success("AI response generated",ai.chat(jwt.getSubject(),body),cid(request));
    }
    private String cid(HttpServletRequest request) { return request.getHeader(CorrelationIdFilter.HEADER_NAME); }
}
