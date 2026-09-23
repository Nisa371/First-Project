package com.marketplace.ai;

import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.*;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiEvaluationConfiguration.Properties.class)
public class AiEvaluationConfiguration {
    @ConfigurationProperties("app.ai")
    @lombok.Getter @lombok.Setter
    public static class Properties {
        private String provider = "UNCONFIGURED";
        private String apiKey = "";
        private String model = "";
        private String baseUrl = "";
        private java.time.Duration timeout = java.time.Duration.ofSeconds(30);
    }
    // Configuration alone never enables a remote provider. A future adapter supplies this bean.
    @Bean @ConditionalOnMissingBean(AiTradeAssistantProvider.class)
    AiTradeAssistantProvider aiTradeAssistantProvider() { return new UnconfiguredAiTradeAssistantProvider(); }
    @Bean @ConditionalOnMissingBean(AiAssessmentProvider.class)
    AiAssessmentProvider aiAssessmentProvider() { return new UnconfiguredAiAssessmentProvider(); }
    @Bean @ConditionalOnMissingBean(AiEvaluationProvider.class)
    AiEvaluationProvider aiEvaluationProvider() { return new UnconfiguredAiEvaluationProvider(); }
}
