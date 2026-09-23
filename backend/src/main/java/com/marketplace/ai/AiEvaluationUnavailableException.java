package com.marketplace.ai;

public class AiEvaluationUnavailableException extends RuntimeException {
    public AiEvaluationUnavailableException() { super("AI_PROVIDER_NOT_CONFIGURED"); }
}
