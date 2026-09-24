package com.marketplace.ai.gemini;

import com.google.genai.Client;
import com.marketplace.ai.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class GeminiConfigurationTest {
    final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(AiEvaluationConfiguration.class)
        .withBean(ObjectMapper.class, () -> JsonMapper.builder().build());
    @Test void defaultAndExplicitUnconfiguredNeedNoKeyOrClient() {
        for (String value : new String[]{"UNCONFIGURED", "unconfigured"}) runner.withPropertyValues("app.ai.provider="+value).run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(Client.class);
            assertThat(context.getBean(AiEvaluationProvider.class)).isInstanceOf(UnconfiguredAiEvaluationProvider.class);
            assertThat(context.getBean(AiAssessmentProvider.class)).isInstanceOf(UnconfiguredAiAssessmentProvider.class);
            assertThat(context.getBean(AiTradeAssistantProvider.class)).isInstanceOf(UnconfiguredAiTradeAssistantProvider.class);
        });
        runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(Client.class));
    }
    @Test void selectedGeminiHasExactlyOneOfEachProviderAndSharedClient() {
        runner.withPropertyValues("app.ai.provider=GEMINI", "app.ai.gemini.api-key=test-only-not-a-real-key").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(Client.class).hasSingleBean(AiEvaluationProvider.class)
                .hasSingleBean(AiAssessmentProvider.class).hasSingleBean(AiTradeAssistantProvider.class);
            assertThat(context.getBean(AiEvaluationProvider.class)).isInstanceOf(GeminiEvaluationProvider.class);
            assertThat(context.getBean(AiAssessmentProvider.class)).isInstanceOf(GeminiAssessmentProvider.class);
            assertThat(context.getBean(AiTradeAssistantProvider.class)).isInstanceOf(GeminiTradeAssistantProvider.class);
        });
    }
    @Test void missingKeyFailsClearlyWithoutNetworkCalls() {
        runner.withPropertyValues("app.ai.provider=GEMINI", "app.ai.gemini.api-key= ").run(context ->
            assertThat(context.getStartupFailure()).hasRootCauseMessage("GEMINI_API_KEY is required when AI_PROVIDER=GEMINI"));
    }
}
