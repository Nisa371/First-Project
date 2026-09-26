package com.marketplace.notification;
import com.marketplace.replacement.MarketplaceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
/** Existing marketplace events remain atomic; optional workflow notices run after commit. */
@lombok.extern.slf4j.Slf4j
@Component
@RequiredArgsConstructor
public class MarketplaceNotificationListener {
    private final NotificationRepository notifications;
    private final NotificationService service;
    @org.springframework.transaction.event.TransactionalEventListener
    public void on(NotificationService.Requested event) {
        try {
            service.deliver(event);
        } catch (RuntimeException e) {
            // Includes transaction commit failures; never log private payloads.
            log.warn("Notification delivery failed for user {}",event.userId());
        }
    }
    @EventListener public void on(MarketplaceEvent e) {
        e.recipients().stream().distinct().forEach(user->{ var n=new Notification(); n.setUser(user); n.setTitle(e.title()); n.setMessage(e.message()); notifications.save(n); });
    }
}
