package com.marketplace.ai;

/** Safe, bounded classification only; never retains the SDK exception or its payload. */
public class AiProviderRequestException extends RuntimeException {
    public enum Type { RATE_LIMIT, TIMEOUT, PROVIDER_5XX, NETWORK, PROVIDER_ERROR }
    private final Type type;
    public AiProviderRequestException(Type type) { super("AI_PROVIDER_" + type.name()); this.type = type; }
    public Type type() { return type; }
}
