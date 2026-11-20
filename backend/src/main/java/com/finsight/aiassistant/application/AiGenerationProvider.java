package com.finsight.aiassistant.application;

import com.finsight.aiassistant.api.AiModelResponse;

public interface AiGenerationProvider {
    AiModelResponse generate(String systemPrompt, String userPrompt);
    String modelName();
}
