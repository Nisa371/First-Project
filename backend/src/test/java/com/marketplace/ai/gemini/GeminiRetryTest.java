package com.marketplace.ai.gemini;

import com.google.genai.Models;
import com.google.genai.errors.ApiException;
import com.google.genai.types.*;
import com.marketplace.ai.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeminiRetryTest {
    final Models models = mock(Models.class);
    final AiEvaluationConfiguration.Properties properties = new AiEvaluationConfiguration.Properties();
    final AtomicLong clock = new AtomicLong();
    final List<Long> delays = new ArrayList<>();
    final GeminiClientGateway gateway = new GeminiClientGateway(models, properties, JsonMapper.builder().build(),
        clock::get, millis -> { delays.add(millis); clock.addAndGet(millis * 1_000_000); }, () -> 125);

    org.mockito.stubbing.OngoingStubbing<GenerateContentResponse> call() {
        return when(models.generateContent(anyString(), org.mockito.ArgumentMatchers.<List<Content>>any(), any(GenerateContentConfig.class)));
    }
    String generate() { return gateway.generate("TRADE_ASSISTANCE", "PRIVATE_PROMPT", "PRIVATE_CV", "PRIVATE_TRANSCRIPT", GeminiClientGateway.Output.TEXT); }
    ApiException error(int status) { return new ApiException(status, "PRIVATE_STATUS", "PRIVATE_API_KEY"); }
    void calls(int count) {
        verify(models, times(count)).generateContent(anyString(), org.mockito.ArgumentMatchers.<List<Content>>any(), any(GenerateContentConfig.class));
        verifyNoMoreInteractions(models);
    }
    @ParameterizedTest @ValueSource(ints={408,429,500,502,503,504})
    void transientThenSuccess(int status) {
        call().thenThrow(error(status)).thenReturn(GeminiTestConfiguration.response("Success"));
        assertThat(generate()).isEqualTo("Success");
        calls(2);
        assertThat(delays).containsExactly(1125L);
    }
    @Test void twoFailuresThenSuccessAndBudgetDecreases() {
        call().thenThrow(error(500),error(503)).thenReturn(GeminiTestConfiguration.response("Success"));
        assertThat(generate()).isEqualTo("Success");
        var configs = ArgumentCaptor.forClass(GenerateContentConfig.class);
        verify(models,times(3)).generateContent(anyString(), org.mockito.ArgumentMatchers.<List<Content>>any(), configs.capture());
        assertThat(delays).containsExactly(1125L,2125L);
        assertThat(configs.getAllValues()).extracting(c -> c.httpOptions().orElseThrow().timeout().orElseThrow())
            .containsExactly(30000,28875,26750);
        assertThat(configs.getAllValues()).allSatisfy(c -> assertThat(c.httpOptions().orElseThrow().retryOptions().orElseThrow().attempts()).contains(1));
    }
    @Test void exhaustedRetriesAreControlled() {
        call().thenThrow(error(503));
        assertThatThrownBy(this::generate).isInstanceOf(AiProviderRequestException.class).hasMessage("AI_PROVIDER_PROVIDER_5XX").hasNoCause();
        calls(3);
        assertThat(delays).containsExactly(1125L,2125L);
    }
    @ParameterizedTest @ValueSource(ints={400,401,403,404,409,422,501,505})
    void permanentStatusesNeverRetry(int status) {
        call().thenThrow(error(status));
        assertThatThrownBy(this::generate).isInstanceOf(AiProviderRequestException.class).hasNoCause();
        calls(1); assertThat(delays).isEmpty();
    }
    @Test void transportIoCanRetry() {
        call().thenThrow(new com.google.genai.errors.GenAiIOException("PRIVATE",new java.io.IOException("PRIVATE")))
            .thenReturn(GeminiTestConfiguration.response("Success"));
        assertThat(generate()).isEqualTo("Success"); calls(2);
    }
    @Test void permanentLocalErrorsNeverRetry() {
        for (RuntimeException error : List.of(new IllegalArgumentException("PRIVATE"),
            new org.springframework.security.access.AccessDeniedException("PRIVATE"),
            new AiProviderInvalidResponseException(),
            new RuntimeException(new javax.net.ssl.SSLHandshakeException("PRIVATE")),
            new RuntimeException(new java.net.ProtocolException("PRIVATE")))) {
            reset(models); delays.clear(); call().thenThrow(error);
            assertThatThrownBy(this::generate).isInstanceOf(RuntimeException.class).hasNoCause();
            calls(1); assertThat(delays).isEmpty();
        }
    }
    @Test void cannotFitBackoffStopsWithoutAnotherCall() {
        properties.setTimeout(Duration.ofSeconds(1)); call().thenThrow(error(503));
        assertThatThrownBy(this::generate).hasMessage("AI_PROVIDER_PROVIDER_5XX");
        calls(1); assertThat(delays).isEmpty();
    }
    @Test void exhaustedCallBudgetDoesNotRetry() {
        properties.setTimeout(Duration.ofSeconds(2));
        call().thenAnswer(invocation -> { clock.addAndGet(2_000_000_000L); throw new RuntimeException(new java.net.SocketTimeoutException()); });
        assertThatThrownBy(this::generate).hasMessage("AI_PROVIDER_TIMEOUT");
        calls(1); assertThat(delays).isEmpty();
    }
    @Test void lateResponseCannotSucceedAfterDeadline() {
        call().thenAnswer(invocation -> { clock.addAndGet(31_000_000_000L); return GeminiTestConfiguration.response("Late"); });
        assertThatThrownBy(this::generate).hasMessage("AI_PROVIDER_TIMEOUT"); calls(1);
    }
    @Test void oversleptBackoffCannotStartAnotherCall() {
        var oversleep = new GeminiClientGateway(models,properties,JsonMapper.builder().build(),clock::get,
            millis -> clock.addAndGet(31_000_000_000L), () -> 0);
        call().thenThrow(error(503));
        assertThatThrownBy(() -> oversleep.generate("TRADE_ASSISTANCE","rules",Map.of(),Map.of(),GeminiClientGateway.Output.TEXT))
            .hasMessage("AI_PROVIDER_TIMEOUT");
        calls(1);
    }
    @Test void transientThenMalformedOutputStopsAtSecondCall() {
        call().thenThrow(error(503)).thenReturn(GeminiTestConfiguration.response("not JSON"));
        assertThatThrownBy(() -> gateway.generate("CV_EVALUATION","rules",Map.of(),Map.of(),GeminiClientGateway.Output.SCORE))
            .isInstanceOf(AiProviderInvalidResponseException.class).hasNoCause();
        calls(2); assertThat(delays).containsExactly(1125L);
    }
    @Test void interruptedBackoffStopsAndPreservesInterrupt() {
        var interrupted = new GeminiClientGateway(models,properties,JsonMapper.builder().build(),clock::get,
            millis -> { throw new InterruptedException(); }, () -> 0);
        call().thenThrow(error(503));
        try {
            assertThatThrownBy(() -> interrupted.generate("TRADE_ASSISTANCE","rules",Map.of(),Map.of(),GeminiClientGateway.Output.TEXT))
                .hasMessage("AI_PROVIDER_TIMEOUT");
            assertThat(Thread.currentThread().isInterrupted()).isTrue(); calls(1);
        } finally { Thread.interrupted(); }
    }
    @Test void logsCorrelateAttemptsAndNeverContainPrivateData() {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(GeminiClientGateway.class);
        var logs = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        logs.start(); logger.addAppender(logs);
        try (var ignored = org.slf4j.MDC.putCloseable("requestId","test-correlation")) {
            call().thenThrow(error(500),error(503)).thenReturn(GeminiTestConfiguration.response("PRIVATE_ANSWER"));
            assertThat(generate()).isEqualTo("PRIVATE_ANSWER");
            assertThat(logs.list).hasSize(3).allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).contains("requestId=test-correlation", "operation=TRADE_ASSISTANCE").doesNotContain("PRIVATE");
                assertThat(event.getThrowableProxy()).isNull();
            });
            assertThat(logs.list.get(0).getFormattedMessage()).contains("attempt=1 status=retry", "providerErrorType=PROVIDER_5XX upstreamStatus=500");
            assertThat(logs.list.get(1).getFormattedMessage()).contains("attempt=2 status=retry", "upstreamStatus=503");
            assertThat(logs.list.get(2).getFormattedMessage()).contains("attempt=3 status=success providerErrorType=NONE");
        } finally { logger.detachAppender(logs); logs.stop(); }
    }
}
