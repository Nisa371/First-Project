package com.marketplace.notification;
import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.CandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository notifications;
    private final CurrentAccount current;
    private final org.springframework.context.ApplicationEventPublisher events;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    public record Requested(Long userId, String title, String message) {}
    // Only business transitions publish this event; reads never create notifications.
    public void afterCommit(Long userId, String title, String message) {
        events.publishEvent(new Requested(userId, title, message));
    }
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void deliver(Requested event) {
        var n=new Notification();
        n.setUser(entityManager.getReference(com.marketplace.user.User.class,event.userId()));
        n.setTitle(event.title()); n.setMessage(event.message()); notifications.save(n);
    }
    public record View(Long id,String title,String message,Instant createdAt,Instant readAt) {}
    public List<View> mine() { return notifications.findByUserIdOrderByCreatedAtDescIdDesc(current.requireActive().getId()).stream().map(n->new View(n.getId(),n.getTitle(),n.getMessage(),n.getCreatedAt(),n.getReadAt())).toList(); }
    public void read(Long id) { var n=notifications.findByIdAndUserId(id,current.requireActive().getId()).orElseThrow(CandidateService::missing); if(n.getReadAt()==null) n.setReadAt(Instant.now()); }
    public void readAll() { var now=Instant.now(); notifications.findByUserIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(current.requireActive().getId()).forEach(n->n.setReadAt(now)); }
}
