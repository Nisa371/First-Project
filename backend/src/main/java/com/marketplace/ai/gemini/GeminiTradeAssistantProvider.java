package com.marketplace.ai.gemini;

import com.marketplace.ai.*;
import com.marketplace.tradeassistant.TradeAssistantDtos.ProviderRequest;
import java.util.Map;

public class GeminiTradeAssistantProvider implements AiTradeAssistantProvider {
    private final GeminiClientGateway gateway;
    public GeminiTradeAssistantProvider(GeminiClientGateway gateway) { this.gateway=gateway; }
    public String reply(ProviderRequest request) {
        String answer = gateway.generate("TRADE_ASSISTANCE", request.systemInstruction(), request.context(),
            Map.of("recentMessages", request.recentMessages(), "userMessage", request.userMessage()), GeminiClientGateway.Output.TEXT);
        if (answer == null || answer.isBlank() || answer.length() > 4000) throw new AiProviderInvalidResponseException();
        return answer.strip();
    }
}
