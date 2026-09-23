package com.marketplace.tradeassistant;
import com.marketplace.ai.*;
import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.*;
import com.marketplace.common.api.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import static com.marketplace.tradeassistant.TradeAssistantDtos.*;
@Service @PreAuthorize("hasRole('CANDIDATE')")
@Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class TradeAssistantService {
    private final CurrentAccount current;
    private final CandidateProfileRepository candidates;
    private final TradeAssistantRepository conversations;
    private final AiTradeAssistantProvider provider;
    private final TradeGuidance guidance;
    private final int limit;
    public TradeAssistantService(CurrentAccount current, CandidateProfileRepository candidates, TradeAssistantRepository conversations,
        AiTradeAssistantProvider provider, TradeGuidance guidance, @Value("${app.trade-assistant.recent-message-limit:12}") int limit) {
        this.current=current; this.candidates=candidates; this.conversations=conversations; this.provider=provider; this.guidance=guidance;
        if(limit<2 || limit>20 || limit%2!=0) throw new IllegalArgumentException("Trade assistant message limit must be even, between 2 and 20");
        this.limit=limit;
    }
    private CandidateProfile owner() {
        var c=candidates.findByUserId(current.requireActive().getId()).orElseThrow(CandidateService::missing);
        if(c.getCandidateType()!=CandidateType.TRADE) throw new ApiException(403,"TRADE_REQUIRED","এই সহকারী ট্রেড প্রার্থীদের জন্য।");
        return c;
    }
    public View get() { return view(conversations.findById(owner().getId()).orElse(null),null); }
    public View reply(Input input) {
        var c=owner(); c=candidates.findByIdForUpdate(c.getId()).orElseThrow(CandidateService::missing);
        var conversation=conversations.findById(c.getId()).orElseGet(TradeAssistantConversation::new);
        String answer;
        try {
            answer=provider.reply(new ProviderRequest(TradeGuidance.INSTRUCTION, guidance.build(c), view(conversation,null).messages(),input.message().strip()));
            if(answer==null || answer.isBlank() || answer.length()>4000) return view(conversation,"AI_PROVIDER_INVALID_RESPONSE");
        } catch(AiEvaluationUnavailableException e) { return view(conversation,"AI_PROVIDER_NOT_CONFIGURED"); }
        catch(RuntimeException e) { return view(conversation,"AI_PROVIDER_UNAVAILABLE"); }
        conversation.setCandidateId(c.getId());
        conversation.getMessages().add(new TradeAssistantConversation.Entry("USER",input.message().strip()));
        conversation.getMessages().add(new TradeAssistantConversation.Entry("ASSISTANT",answer.strip()));
        while(conversation.getMessages().size()>limit) conversation.getMessages().removeFirst();
        conversations.save(conversation);
        return view(conversation,null);
    }
    private View view(TradeAssistantConversation c,String failure) {
        return new View(failure,c==null?List.of():c.getMessages().stream().skip(Math.max(0,c.getMessages().size()-limit))
            .map(m -> new Message(m.getRole(),m.getContent())).toList());
    }
}
