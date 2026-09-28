package com.marketplace.placement;

import com.marketplace.auth.CurrentAccount;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReplacementGuaranteePolicyService implements ApplicationRunner {
    private final ReplacementGuaranteePolicyRepository policies;
    private final CurrentAccount current;
    public record Policy(@NotNull @Min(1) @Max(3650) Integer days) {}

    @Override
    public void run(ApplicationArguments args) {
        if (!policies.existsById(1L)) policies.save(new ReplacementGuaranteePolicy());
    }

    public int days() { return policies.findById(1L).orElseThrow().getDays(); }

    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYER')")
    public Policy read() { current.requireActive(); return new Policy(days()); }

    @PreAuthorize("hasRole('ADMIN')")
    public Policy update(Policy request) {
        current.requireActive();
        var policy = policies.findById(1L).orElseThrow();
        policy.setDays(request.days());
        return new Policy(policy.getDays());
    }
}
