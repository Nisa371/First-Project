package com.marketplace.companytype;

import com.marketplace.common.api.ApiException;
import com.marketplace.employer.*;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class CompanyTypeService {
    private final CompanyTypeRepository types;
    private final EmployerProfileRepository employers;
    private final com.marketplace.verification.VerificationRequirementRepository requirements;
    public record View(Long id, String name, boolean active, boolean other) {}
    public record Input(@NotBlank @Size(max=120) String name, @NotNull Boolean active) {}
    public static String clean(String value) { return value == null ? "" : value.replaceAll("(?U)\\s+", " ").strip(); }
    public static String key(String value) { return clean(value).toLowerCase(Locale.ROOT); }
    public static View view(CompanyType c) { return new View(c.getId(), c.getName(), c.isActive(), c.isOther()); }
    public List<View> selectable() { return types.findByActiveTrueOrderByNameAsc().stream().map(CompanyTypeService::view).toList(); }
    @PreAuthorize("hasRole('ADMIN')")
    public List<View> all() { return types.findAll(org.springframework.data.domain.PageRequest.of(0,50,org.springframework.data.domain.Sort.by("name"))).stream().map(CompanyTypeService::view).toList(); }
    @PreAuthorize("hasRole('ADMIN')")
    public View create(Input input) { return save(new CompanyType(), input); }
    @PreAuthorize("hasRole('ADMIN')")
    public View update(Long id, Input input) { return save(require(id), input); }
    private View save(CompanyType type, Input input) {
        String name = clean(input.name());
        if (name.isBlank() || name.length() > 120) throw invalid("Enter a company type of 1–120 characters.");
        if (type.isOther() && (!"Other".equals(name) || !Boolean.TRUE.equals(input.active())))
            throw invalid("Other must keep its name and remain active.");
        types.findByNormalizedName(key(name)).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), type.getId()))
                throw new ApiException(409, "COMPANY_TYPE_EXISTS", "This company type already exists, possibly inactive.");
        });
        type.setName(name); type.setNormalizedName(key(name)); type.setActive(Boolean.TRUE.equals(input.active()));
        return view(types.saveAndFlush(type));
    }
    @PreAuthorize("hasRole('ADMIN')")
    public void remove(Long id) {
        var type = require(id);
        if (type.isOther()) throw invalid("Other cannot be removed.");
        if ("HOUSEHOLD".equals(type.getCode()) || employers.existsByCompanyTypeId(id) || requirements.existsByCompanyTypeId(id)) type.setActive(false);
        else types.delete(type);
    }
    // Locking serializes selection with deactivation/deletion, including new registrations.
    public void select(EmployerProfile employer, Long id, String custom) {
        if (id == null) throw invalid("Select a company type.");
        var type = require(id);
        if (!type.isActive() && (employer.getCompanyType() == null || !id.equals(employer.getCompanyType().getId())))
            throw invalid("This company type is inactive. Select an available company type.");
        String value = clean(custom);
        if (type.isOther() && (value.isBlank() || value.length() > 120))
            throw invalid("Specify a company type of 1–120 characters when selecting Other.");
        if (!type.isOther() && !value.isEmpty()) throw invalid("Custom company type is only allowed for Other.");
        employer.setCompanyType(type); employer.setCustomCompanyType(type.isOther() ? value : null);
    }
    private CompanyType require(Long id) { return types.lockById(id).orElseThrow(() -> invalid("The selected company type does not exist.")); }
    private static ApiException invalid(String message) { return new ApiException(400, "VALIDATION_ERROR", message); }
}
