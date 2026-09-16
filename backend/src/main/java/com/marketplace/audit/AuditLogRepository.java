package com.marketplace.audit;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface AuditLogRepository extends Repository<AuditLog, Long> {
    AuditLog save(AuditLog auditLog);
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(String entityType, Long entityId);
}

