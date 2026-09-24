package com.marketplace.ai;

/** Safe provider-independent error; never includes model output or credentials. */
public class AiProviderInvalidResponseException extends RuntimeException {
    public AiProviderInvalidResponseException() { super("INVALID_AI_RESPONSE"); }
}
