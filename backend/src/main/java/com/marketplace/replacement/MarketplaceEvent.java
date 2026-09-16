package com.marketplace.replacement;
import com.marketplace.user.User;
import java.util.List;
/** Immutable event payload contains only allowlisted operational text, never private evidence. */
public record MarketplaceEvent(User actor, String action, String entityType, Long entityId,
    List<User> recipients, String title, String message) {}
