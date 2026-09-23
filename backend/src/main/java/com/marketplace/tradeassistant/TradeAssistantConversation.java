package com.marketplace.tradeassistant;
import jakarta.persistence.*;
import java.util.*;
@Entity @Table(name="trade_assistant_conversations") @lombok.Getter @lombok.Setter
public class TradeAssistantConversation {
    @Id private Long candidateId;
    @ElementCollection @CollectionTable(name="trade_assistant_messages", joinColumns=@JoinColumn(name="candidate_id"))
    @OrderColumn(name="message_order") private List<Entry> messages = new ArrayList<>();
    @Embeddable @lombok.Getter @lombok.NoArgsConstructor
    public static class Entry {
        @Column(nullable=false, length=16) private String role;
        @Column(nullable=false, length=4000) private String content;
        public Entry(String role, String content) { this.role=role; this.content=content; }
    }
}
