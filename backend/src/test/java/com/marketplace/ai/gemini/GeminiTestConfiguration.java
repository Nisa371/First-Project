package com.marketplace.ai.gemini;

import com.google.genai.Models;
import com.google.genai.types.*;
import com.marketplace.ai.AiEvaluationConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import static org.mockito.Mockito.*;

@TestConfiguration(proxyBeanMethods=false)
public class GeminiTestConfiguration {
    @Bean Models testGeminiModels() { return mock(Models.class); }
    @Bean @Primary GeminiClientGateway testGeminiGateway(Models models, AiEvaluationConfiguration.Properties properties, ObjectMapper mapper) {
        return new GeminiClientGateway(models, properties, mapper, System::nanoTime, millis -> {}, () -> 0);
    }
    public static GenerateContentResponse response(String text) {
        return GenerateContentResponse.builder().candidates(Candidate.builder().finishReason("STOP")
            .content(Content.fromParts(Part.fromText(text))).build()).build();
    }
    public static void respond(Models models, String text) {
        when(models.generateContent(anyString(), org.mockito.ArgumentMatchers.<List<Content>>any(), any(GenerateContentConfig.class)))
            .thenReturn(response(text));
    }
}
