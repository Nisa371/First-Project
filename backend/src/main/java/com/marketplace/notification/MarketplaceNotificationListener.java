package com.marketplace.notification;
import com.marketplace.replacement.MarketplaceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
/** Synchronous Observer: notifications commit/rollback with the workflow transaction. */
@Component
@RequiredArgsConstructor
public class MarketplaceNotificationListener {
    private final NotificationRepository notifications;
    @EventListener public void on(MarketplaceEvent e) {
        e.recipients().stream().distinct().forEach(user->{ var n=new Notification(); n.setUser(user); n.setTitle(e.title()); n.setMessage(e.message()); notifications.save(n); });
    }
}
