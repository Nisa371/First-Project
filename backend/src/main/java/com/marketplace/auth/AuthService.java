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
    private final jakarta.validation.Validator validator;
    private final CandidateProfileRepository candidates;
    private final EmployerProfileRepository employers;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final AuditService audit;
    private final CurrentAccount current;
    private final com.marketplace.companytype.CompanyTypeService companyTypes;

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
        var identifier = identifier(request.identifier());
        if (identifier.email() != null ? users.existsByEmailIgnoreCase(identifier.email()) : users.existsByPhone(identifier.phone())) {
            throw alreadyRegistered(identifier);
        }
        var user = new User();
        user.setEmail(identifier.email());
        user.setPhone(identifier.phone());
        user.setPasswordHash(passwords.encode(request.password()));
        user.setRole(candidate ? Role.CANDIDATE : Role.EMPLOYER);
        try {
            user = users.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                        && violation.getConstraintName() != null
                        && (violation.getConstraintName().contains("uk_user_email") || violation.getConstraintName().contains("uk_user_phone"))) {
                    throw alreadyRegistered(identifier);
                }
            }
            throw e;
        }
        if (candidate) {
            var profile = new CandidateProfile();
            profile.setUser(user);
            profile.setPhone(user.getPhone());
            profile.setContactEmail(user.getEmail());
            profile.setCandidateType(request.candidateType());
            profile.setFullName(request.fullName().strip());
            candidates.save(profile);
        } else {
            var profile = new EmployerProfile();
            profile.setUser(user);
            profile.setCompanyName(request.companyName().strip());
            companyTypes.select(profile, request.companyTypeId(), request.customCompanyType());
            employers.save(profile);
        }
        audit.recordAccountEvent(user, Action.ACCOUNT_REGISTERED);
        return response(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        var identifier = identifier(request.identifier());
        var user = (identifier.email() != null ? users.findByEmailIgnoreCase(identifier.email()) : users.findByPhone(identifier.phone())).orElse(null);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72
                || user == null || !passwords.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(401, "INVALID_CREDENTIALS", "Email/phone or password is incorrect.");
        }
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(403, "ACCOUNT_INACTIVE", "Your account is not active. Contact the platform team.");
        }
        audit.recordAccountEvent(user, Action.LOGIN_SUCCEEDED);
        return response(user);
    }

    @Transactional(readOnly = true)
    public CurrentUser me() { return summary(current.requireActive()); }

    private record Identifier(@jakarta.validation.constraints.Email String email, String phone) {}

    private Identifier identifier(String input) {
        String value = input == null ? "" : input.strip();
        if (value.contains("@")) {
            var result = new Identifier(value.toLowerCase(Locale.ROOT), null);
            if (value.length() <= 254 && validator.validate(result).isEmpty()) return result;
        } else if (normalizePhone(value) != null) {
            return new Identifier(null, normalizePhone(value));
        }
        throw new ApiException(400, "VALIDATION_ERROR", "Enter a valid email address or phone number.");
    }

    public static String normalizePhone(String input) {
        String value = input == null ? "" : input.strip();
        if (!value.matches("(?:\\+?88)?01[3-9][0-9]{8}")) return null;
        return value.startsWith("+") ? value : value.startsWith("88") ? "+" + value : "+88" + value;
    }

    private ApiException alreadyRegistered(Identifier identifier) {
        return identifier.email() != null
                ? new ApiException(409, "EMAIL_IN_USE", "This email is already registered.")
                : new ApiException(409, "PHONE_IN_USE", "This phone number is already registered.");
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwt.issue(user.getId()), "Bearer", jwt.ttlSeconds(), summary(user));
    }

    private CurrentUser summary(User user) {
        if (user.getRole() == Role.CANDIDATE) {
            var profile = candidates.findByUserId(user.getId()).orElseThrow();
            return new CurrentUser(user.getId(), user.getEmail(), user.getPhone(), user.getRole(), profile.getCandidateType(), profile.getFullName());
        }
        String name = user.getRole() == Role.EMPLOYER
                ? employers.findByUserId(user.getId()).orElseThrow().getCompanyName() : user.getRole().name();
        return new CurrentUser(user.getId(), user.getEmail(), user.getPhone(), user.getRole(), null, name);
    }
}
