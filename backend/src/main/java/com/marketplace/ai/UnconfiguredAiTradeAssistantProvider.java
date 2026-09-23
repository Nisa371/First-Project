package com.marketplace.ai;
import com.marketplace.tradeassistant.TradeAssistantDtos.ProviderRequest;
public class UnconfiguredAiTradeAssistantProvider implements AiTradeAssistantProvider {
    public String reply(ProviderRequest request) { throw new AiEvaluationUnavailableException(); }
}
