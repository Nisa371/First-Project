package com.marketplace.verification;
import com.marketplace.companytype.CompanyTypeRepository;
import com.marketplace.companytype.CompanyTypeService;
import com.marketplace.common.api.ApiException;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional
@PreAuthorize("hasRole('ADMIN')")
public class VerificationRequirements {
    private final VerificationRequirementRepository requirements;
    private final CompanyTypeRepository types;
    public record Input(@NotBlank @Size(max=160) String name, @Size(max=2000) String description,
        @NotNull VerificationTarget targetType, Long companyTypeId, @NotNull Boolean required, @NotNull Boolean active) {}
    public record View(Long id, String name, String description, VerificationTarget targetType, Long companyTypeId,
        String companyTypeName, boolean required, boolean active, boolean baseline) {}
    public static View view(VerificationRequirement r) { return new View(r.getId(),r.getName(),r.getDescription(),r.getTargetType(),
        r.getCompanyType()==null?null:r.getCompanyType().getId(),r.getCompanyType()==null?null:r.getCompanyType().getName(),r.isRequired(),r.isActive(),r.getCode()!=null); }
    public List<View> all() { return requirements.findAll(org.springframework.data.domain.PageRequest.of(0,50,org.springframework.data.domain.Sort.by("id"))).stream().map(VerificationRequirements::view).toList(); }
    public View create(Input input) { return save(new VerificationRequirement(),input); }
    public View update(Long id, Input input) { return save(requirements.findById(id).orElseThrow(VerificationChecklist::missing),input); }
    private View save(VerificationRequirement r, Input i) {
        String name=CompanyTypeService.clean(i.name());
        if(name.isBlank()) throw invalid("Enter a requirement name.");
        if(i.companyTypeId()!=null && i.targetType()==VerificationTarget.CANDIDATE) throw invalid("Company types can only target employers.");
        if(r.getCode()!=null && (!i.active() || !i.required() || i.targetType()!=r.getTargetType() || i.companyTypeId()!=null))
            throw invalid("Baseline requirements must remain active, required and keep their original target.");
        var type=i.companyTypeId()==null?null:types.lockById(i.companyTypeId()).orElseThrow(VerificationChecklist::missing);
        if(type!=null && ((i.targetType()==VerificationTarget.HOUSEHOLD_EMPLOYER && !"HOUSEHOLD".equals(type.getCode()))
            || (i.targetType()==VerificationTarget.COMPANY_EMPLOYER && "HOUSEHOLD".equals(type.getCode()))))
            throw invalid("The company type does not match this target.");
        r.setName(name);r.setDescription(i.description());r.setTargetType(i.targetType());r.setCompanyType(type);r.setRequired(i.required());r.setActive(i.active());
        return view(requirements.saveAndFlush(r));
    }
    private static ApiException invalid(String message) { return new ApiException(400,"VALIDATION_ERROR",message); }
}
