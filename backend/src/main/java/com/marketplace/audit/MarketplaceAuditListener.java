package com.marketplace.audit;
import com.marketplace.replacement.MarketplaceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class MarketplaceAuditListener {
    private final AuditLogRepository logs;
    @EventListener public void on(MarketplaceEvent e) { var log=new AuditLog(); log.setActorUser(e.actor()); log.setAction(e.action()); log.setEntityType(e.entityType()); log.setEntityId(e.entityId()); logs.save(log); }
}
