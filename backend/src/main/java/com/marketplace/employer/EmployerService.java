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
    private final com.marketplace.companytype.CompanyTypeService companyTypes;
    public record Update(@NotBlank @Size(max=200) String companyName, @NotNull Long companyTypeId, @Size(max=120) String customCompanyType,
        @Size(max=32) @Pattern(regexp="[+0-9 ()-]*") String contactPhone,
        @Size(max=500) String address, @Size(max=2000) String description) {}
    public record Profile(String companyName, Long companyTypeId, String companyTypeName, boolean companyTypeOther,
        boolean companyTypeActive, String customCompanyType, String contactPhone, String address, String description) {}
    public EmployerProfile own() { return employers.findByUserId(current.requireActive().getId()).orElseThrow(CandidateService::missing); }
    public Profile profile() { return view(own()); }
    public Profile update(Update r) {
        return apply(own(), r);
    }
    @PreAuthorize("hasRole('ADMIN')")
    public Profile adminUpdate(Long id, Update r) {
        current.requireActive(); return apply(employers.findById(id).orElseThrow(CandidateService::missing), r);
    }
    private Profile apply(EmployerProfile e, Update r) {
        e.setCompanyName(r.companyName().trim()); companyTypes.select(e, r.companyTypeId(), r.customCompanyType()); e.setContactPhone(r.contactPhone());
        e.setAddress(r.address()); e.setDescription(r.description()); return view(e);
    }
    private Profile view(EmployerProfile e) {
        var type = e.getCompanyType();
        return new Profile(e.getCompanyName(), type == null ? null : type.getId(), type == null ? null : type.getName(),
            type != null && type.isOther(), type != null && type.isActive(), e.getCustomCompanyType(), e.getContactPhone(), e.getAddress(), e.getDescription());
    }
}
