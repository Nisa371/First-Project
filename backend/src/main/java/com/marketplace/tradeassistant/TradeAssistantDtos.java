package com.marketplace.tradeassistant;
import jakarta.validation.constraints.*;
import java.util.List;
public final class TradeAssistantDtos {
    private TradeAssistantDtos() {}
    public record Input(@NotBlank @Size(max=2000) String message) {}
    public record Message(String role, String content) {}
    public record Document(String name, boolean required, String status) {}
    public record Context(String role, String segment, String verificationStatus, boolean profileComplete,
        List<Document> documents, List<String> workflows) {}
    public record ProviderRequest(String systemInstruction, Context context, List<Message> recentMessages, String userMessage) {}
    public record View(String failureCode, List<Message> messages) {}
}
