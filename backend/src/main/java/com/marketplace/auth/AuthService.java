package com.marketplace.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import com.marketplace.auth.AuthDtos.*;
import com.marketplace.audit.AuditService;
import com.marketplace.audit.AuditService.Action;
import com.marketplace.candidate.*;
import com.marketplace.employer.*;
import com.marketplace.common.api.ApiException;
import com.marketplace.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final CandidateProfileRepository candidates;
    private final EmployerProfileRepository employers;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final AuditService audit;
    private final CurrentAccount current;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(400, "VALIDATION_ERROR", "Password must be at most 72 UTF-8 bytes.");
        }
        boolean candidate = request.accountType() == AccountType.CANDIDATE;
        if (candidate && (request.candidateType() == null || request.fullName() == null || request.fullName().isBlank())) {
            throw new ApiException(400, "VALIDATION_ERROR", "Candidate track and full name are required.");
        }
        if (!candidate && (request.companyName() == null || request.companyName().isBlank() || request.candidateType() != null)) {
            throw new ApiException(400, "VALIDATION_ERROR", "Company name is required; candidate track is only for candidates.");
        }
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(409, "EMAIL_IN_USE", "An account with this email already exists. Please sign in.");
        }
        var user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(request.password()));
        user.setRole(candidate ? Role.CANDIDATE : Role.EMPLOYER);
        user = users.saveAndFlush(user);
        if (candidate) {
            var profile = new CandidateProfile();
            profile.setUser(user);
            profile.setCandidateType(request.candidateType());
            profile.setFullName(request.fullName().strip());
            candidates.save(profile);
        } else {
            var profile = new EmployerProfile();
            profile.setUser(user);
            profile.setCompanyName(request.companyName().strip());
            employers.save(profile);
        }
        audit.recordAccountEvent(user, Action.ACCOUNT_REGISTERED);
        return response(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        var user = users.findByEmailIgnoreCase(request.email().strip()).orElse(null);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72
                || user == null || !passwords.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(401, "INVALID_CREDENTIALS", "Email or password is incorrect.");
        }
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(403, "ACCOUNT_INACTIVE", "Your account is not active. Contact the platform team.");
        }
        audit.recordAccountEvent(user, Action.LOGIN_SUCCEEDED);
        return response(user);
    }

    @Transactional(readOnly = true)
    public CurrentUser me() { return summary(current.requireActive()); }

    private AuthResponse response(User user) {
        return new AuthResponse(jwt.issue(user.getId()), "Bearer", jwt.ttlSeconds(), summary(user));
    }

    private CurrentUser summary(User user) {
        if (user.getRole() == Role.CANDIDATE) {
            var profile = candidates.findByUserId(user.getId()).orElseThrow();
            return new CurrentUser(user.getId(), user.getEmail(), user.getRole(), profile.getCandidateType(), profile.getFullName());
        }
        String name = user.getRole() == Role.EMPLOYER
                ? employers.findByUserId(user.getId()).orElseThrow().getCompanyName() : user.getRole().name();
        return new CurrentUser(user.getId(), user.getEmail(), user.getRole(), null, name);
    }
}
