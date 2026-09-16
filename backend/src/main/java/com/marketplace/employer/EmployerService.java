package com.marketplace.employer;
import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.CandidateService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

@Service
@RequiredArgsConstructor
@Transactional
@PreAuthorize("hasRole('EMPLOYER')")
public class EmployerService {
    private final CurrentAccount current;
    private final EmployerProfileRepository employers;
    public record Profile(@NotBlank @Size(max=200) String companyName, @Size(max=120) String industry,
        @Size(max=32) @Pattern(regexp="[+0-9 ()-]*") String contactPhone,
        @Size(max=500) String address, @Size(max=2000) String description) {}
    public EmployerProfile own() { return employers.findByUserId(current.requireActive().getId()).orElseThrow(CandidateService::missing); }
    public Profile profile() { return view(own()); }
    public Profile update(Profile r) {
        var e=own(); e.setCompanyName(r.companyName().trim()); e.setIndustry(r.industry()); e.setContactPhone(r.contactPhone());
        e.setAddress(r.address()); e.setDescription(r.description()); return view(e);
    }
    private Profile view(EmployerProfile e) { return new Profile(e.getCompanyName(),e.getIndustry(),e.getContactPhone(),e.getAddress(),e.getDescription()); }
}
