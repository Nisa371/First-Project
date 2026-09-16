package com.marketplace.auth;

import com.marketplace.common.api.ApiException;
import com.marketplace.user.AccountStatus;
import com.marketplace.user.User;
import com.marketplace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentAccount {
    private final UserRepository users;

    public User requireActive() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long id)) {
            throw new ApiException(401, "UNAUTHORIZED", "Please sign in to continue.");
        }
        var user = users.findById(id).orElseThrow(() -> new ApiException(401, "UNAUTHORIZED", "Please sign in again."));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(403, "ACCOUNT_INACTIVE", "Your account is not active. Contact the platform team.");
        }
        return user;
    }

    public void requireOwner(Long ownerUserId) {
        if (!requireActive().getId().equals(ownerUserId)) {
            throw new ApiException(403, "FORBIDDEN", "You do not have permission to access this resource.");
        }
    }
}
