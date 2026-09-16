package com.marketplace.audit;

import com.marketplace.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Only allowlisted events; never store request bodies, credentials or identity evidence. */
@Service
@RequiredArgsConstructor
public class AuditService {
    public enum Action { ACCOUNT_REGISTERED, LOGIN_SUCCEEDED }
    private final AuditLogRepository logs;

    @Transactional
    public void recordAccountEvent(User actor, Action action) {
        var log = new AuditLog();
        log.setActorUser(actor);
        log.setAction(action.name());
        log.setEntityType("USER");
        log.setEntityId(actor.getId());
        logs.save(log);
    }
}
