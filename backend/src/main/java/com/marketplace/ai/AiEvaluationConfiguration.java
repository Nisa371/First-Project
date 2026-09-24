package com.marketplace.ai;

import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.*;

@Configuration(proxyBeanMethods = false)
@Import(com.marketplace.ai.gemini.GeminiConfiguration.class)
@EnableConfigurationProperties(AiEvaluationConfiguration.Properties.class)
public class AiEvaluationConfiguration {
    @ConfigurationProperties("app.ai")
    @lombok.Getter @lombok.Setter
    public static class Properties {
        private String provider = "UNCONFIGURED";
        private String apiKey = "";
        private String model = "";
        private Gemini gemini = new Gemini();
        @lombok.Getter @lombok.Setter
        public static class Gemini {
            private String apiKey = "";
            private String model = "gemini-3.8-flash";
            private String thinkingLevel = "low";
        }
        private String baseUrl = "";
        private java.time.Duration timeout = java.time.Duration.ofSeconds(30);
    }
    // Keep keyless startup and existing test/provider overrides.
    @ConditionalOnProperty(name="app.ai.provider", havingValue="UNCONFIGURED", matchIfMissing=true)
    @Bean @ConditionalOnMissingBean(AiTradeAssistantProvider.class)
    AiTradeAssistantProvider aiTradeAssistantProvider() { return new UnconfiguredAiTradeAssistantProvider(); }
    @ConditionalOnProperty(name="app.ai.provider", havingValue="UNCONFIGURED", matchIfMissing=true)
    @Bean @ConditionalOnMissingBean(AiAssessmentProvider.class)
    AiAssessmentProvider aiAssessmentProvider() { return new UnconfiguredAiAssessmentProvider(); }
    @ConditionalOnProperty(name="app.ai.provider", havingValue="UNCONFIGURED", matchIfMissing=true)
    @Bean @ConditionalOnMissingBean(AiEvaluationProvider.class)
    AiEvaluationProvider aiEvaluationProvider() { return new UnconfiguredAiEvaluationProvider(); }
}
