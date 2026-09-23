package com.marketplace.verification;
import com.marketplace.companytype.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @RequiredArgsConstructor @Order(-90)
public class VerificationCatalog implements ApplicationRunner {
    private final VerificationRequirementRepository requirements;
    private final VerificationRecordRepository records;
    private final CompanyTypeRepository types;
    @Override @Transactional public void run(ApplicationArguments args) {
        // Persist identity separately from the editable company-type label.
        if (types.findByCode("HOUSEHOLD").isEmpty()) {
            var household = types.findByNormalizedName("household / personal employer").orElseGet(() -> {
                var t = new CompanyType(); t.setName("Household / Personal Employer"); t.setNormalizedName("household / personal employer"); return t;
            });
            household.setCode("HOUSEHOLD"); types.save(household);
        }
        seed("CANDIDATE_NID", "National ID", VerificationTarget.CANDIDATE);
        seed("HOUSEHOLD_NID", "National ID", VerificationTarget.HOUSEHOLD_EMPLOYER);
        seed("TRADE_LICENSE", "Trade License", VerificationTarget.COMPANY_EMPLOYER);
        seed("TAX_DOCUMENT", "Tax Document", VerificationTarget.COMPANY_EMPLOYER);
        var nid = requirements.findByCode("CANDIDATE_NID").orElseThrow();
        for (var record : records.findByRequirementIsNull()) {
            if (record.getCandidate() != null) {
                record.setOwner(record.getCandidate().getUser()); record.setRequirement(nid);
            }
        }
    }
    private void seed(String code, String name, VerificationTarget target) {
        if (requirements.findByCode(code).isPresent()) return;
        var r = new VerificationRequirement(); r.setCode(code); r.setName(name); r.setTargetType(target);
        r.setDescription("Upload a clear PDF, JPG or PNG copy, up to 5 MB. Platform/manual verification."); requirements.save(r);
    }
}
