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
            @com.fasterxml.jackson.annotation.JsonAlias("email")
            @NotBlank(message = "Enter a valid email address or phone number.") @Size(max = 254, message = "Enter a valid email address or phone number.") String identifier,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 160) String fullName,
            @Size(max = 200) String companyName,
            Long companyTypeId,
            @Size(max = 120) String customCompanyType) {}
    public record LoginRequest(
            @com.fasterxml.jackson.annotation.JsonAlias("email")
            @NotBlank(message = "Enter a valid email address or phone number.") @Size(max = 254, message = "Enter a valid email address or phone number.") String identifier,
            @NotBlank @Size(max = 72) String password) {}
    public record CurrentUser(Long id, String email, String phone, Role role, CandidateType candidateType, String displayName) {}
    public record AuthResponse(String token, String tokenType, long expiresIn, CurrentUser user) {}
}
