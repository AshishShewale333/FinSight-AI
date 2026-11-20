package com.finsight.aiassistant.application;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiProviderConfiguration {
    @Bean
    @ConditionalOnProperty(name="finsight.ai.enabled", havingValue="true")
    @ConditionalOnBean(ChatClient.Builder.class)
    AiGenerationProvider springAiGenerationProvider(ChatClient.Builder builder) {
        return new SpringAiGenerationProvider(builder.build());
    }

    @Bean
    @ConditionalOnMissingBean(AiGenerationProvider.class)
    AiGenerationProvider unavailableAiGenerationProvider() {
        return new AiGenerationProvider() {
            @Override public com.finsight.aiassistant.api.AiModelResponse generate(String systemPrompt, String userPrompt) {
                throw new com.finsight.common.error.ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "AI_PROVIDER_UNAVAILABLE", "AI chat provider is not configured");
            }
            @Override public String modelName() { return "unavailable"; }
        };
    }
}
