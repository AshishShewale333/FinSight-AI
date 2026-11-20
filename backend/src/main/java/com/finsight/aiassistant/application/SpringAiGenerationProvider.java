package com.finsight.aiassistant.application;

import com.finsight.aiassistant.api.AiModelResponse;
import org.springframework.ai.chat.client.ChatClient;

public class SpringAiGenerationProvider implements AiGenerationProvider {
    private final ChatClient chatClient;
    public SpringAiGenerationProvider(ChatClient chatClient) { this.chatClient=chatClient; }
    @Override
    public AiModelResponse generate(String systemPrompt, String userPrompt) {
        return chatClient.prompt().system(systemPrompt).user(userPrompt).call().entity(AiModelResponse.class);
    }
    @Override public String modelName() { return "openai-compatible"; }
}
