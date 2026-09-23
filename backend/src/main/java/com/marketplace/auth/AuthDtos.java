package com.marketplace.auth;

import com.marketplace.candidate.CandidateType;
import com.marketplace.user.Role;
import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}
    // Public signup choice is deliberately separate from the operational Role enum.
    public enum AccountType { CANDIDATE, EMPLOYER }
    public record RegisterRequest(
            @NotNull AccountType accountType,
            CandidateType candidateType,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 160) String fullName,
            @Size(max = 200) String companyName,
            Long companyTypeId,
            @Size(max = 120) String customCompanyType) {}
    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {}
    public record CurrentUser(Long id, String email, Role role, CandidateType candidateType, String displayName) {}
    public record AuthResponse(String token, String tokenType, long expiresIn, CurrentUser user) {}
}
