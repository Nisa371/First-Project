package com.marketplace.companytype;

import com.marketplace.employer.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @RequiredArgsConstructor @Order(-100)
public class CompanyTypeCatalog implements ApplicationRunner {
    private final CompanyTypeRepository types;
    private final EmployerProfileRepository employers;
    @Override @Transactional
    public void run(ApplicationArguments args) {
        // Seed once: admin renames, deactivations and deletions survive future restarts.
        if (types.count() == 0) {
            for (String name : new String[]{"FMCG", "Pharmaceuticals", "Information Technology", "Software / Technology",
                "Banking / Financial Services", "Telecommunications", "Manufacturing", "Textile / Garments",
                "Construction", "Real Estate", "Education", "Healthcare / Hospital", "Retail", "E-commerce",
                "Logistics / Transportation", "Hospitality / Tourism", "Media / Advertising", "NGO / Development",
                "Government / Public Sector", "Agriculture", "Household / Personal Employer", "Other"}) {
                var c = new CompanyType(); c.setName(name); c.setNormalizedName(CompanyTypeService.key(name));
                if (name.equals("Other")) c.setCode("OTHER");
                types.save(c);
            }
        }
        types.findByCode("OTHER").orElseThrow(() -> new IllegalStateException("Required Other company type is missing"));
        for (var employer : employers.findByCompanyTypeIsNull()) migrate(employer);
    }
    @Transactional
    public void migrate(EmployerProfile employer) {
        if (employer.getCompanyType() != null) return;
        String legacy = CompanyTypeService.clean(employer.getIndustry());
        // Accounts without an old industry can still sign in; require a selection on next profile save.
        if (legacy.isBlank()) return;
        var type = types.findByNormalizedName(CompanyTypeService.key(legacy))
            .orElseGet(() -> types.findByCode("OTHER").orElseThrow());
        employer.setCompanyType(type);
        employer.setCustomCompanyType(type.isOther() ? legacy : null);
        // Keep the original industry column intact as a legacy snapshot.
    }
}
