package com.marketplace.ai.gemini;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.marketplace.ai.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name="app.ai.provider", havingValue="GEMINI")
public class GeminiConfiguration {
    @Bean(destroyMethod="close")
    Client geminiClient(AiEvaluationConfiguration.Properties properties) {
        var settings = properties.getGemini();
        if (settings.getApiKey() == null || settings.getApiKey().isBlank())
            throw new IllegalStateException("GEMINI_API_KEY is required when AI_PROVIDER=GEMINI");
        if (settings.getModel() == null || settings.getModel().isBlank())
            throw new IllegalStateException("GEMINI_MODEL must not be blank");
        long timeout = properties.getTimeout().toMillis();
        if (timeout <= 0 || timeout > Integer.MAX_VALUE)
            throw new IllegalStateException("AI_TIMEOUT must be a positive duration within the SDK timeout range");
        return Client.builder().vertexAI(false).apiKey(settings.getApiKey())
            .httpOptions(HttpOptions.builder().timeout((int) timeout)
                .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build();
    }
    @Bean GeminiClientGateway geminiClientGateway(Client client, AiEvaluationConfiguration.Properties properties, ObjectMapper mapper) {
        return new GeminiClientGateway(client.models, properties, mapper);
    }
    @Bean AiEvaluationProvider geminiEvaluationProvider(GeminiClientGateway gateway) { return new GeminiEvaluationProvider(gateway); }
    @Bean AiAssessmentProvider geminiAssessmentProvider(GeminiClientGateway gateway) { return new GeminiAssessmentProvider(gateway); }
    @Bean AiTradeAssistantProvider geminiTradeAssistantProvider(GeminiClientGateway gateway) { return new GeminiTradeAssistantProvider(gateway); }
}
