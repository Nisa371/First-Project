package com.marketplace.verification;
import com.marketplace.candidate.CandidateProfileRepository;
import com.marketplace.employer.EmployerProfileRepository;
import com.marketplace.common.api.ApiException;
import com.marketplace.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class VerificationChecklist {
    private final VerificationRequirementRepository requirements;
    private final VerificationRecordRepository records;
    private final EmployerProfileRepository employers;
    private final CandidateProfileRepository candidates;
    public record Item(VerificationRequirements.View requirement, Document submission) {}
    public record Document(Long id, VerificationStatus status, String originalName, String requirementName, Long fileSize, String reviewNote,
        java.time.Instant submittedAt, java.time.Instant reviewedAt, boolean hasFile, boolean legacy) {}
    public record Checklist(String status, boolean companyTypeRequired, List<Item> items, List<Document> history) {}
    public User owner(VerificationRecord v) { return v.getOwner()!=null ? v.getOwner() : v.getCandidate().getUser(); }
    public boolean matches(VerificationRecord v, VerificationRequirement r) {
        return v.getRequirement()!=null ? v.getRequirement().getId().equals(r.getId())
            : v.getCandidate()!=null && "CANDIDATE_NID".equals(r.getCode());
    }
    public List<VerificationRequirement> applicable(User user) {
        var employer=user.getRole()==Role.EMPLOYER ? employers.findByUserId(user.getId()).orElseThrow(VerificationChecklist::missing) : null;
        var type=employer==null?null:employer.getCompanyType(); boolean household=type!=null && "HOUSEHOLD".equals(type.getCode());
        return requirements.findByActiveTrueOrderByIdAsc().stream().filter(r -> {
            boolean target=switch(r.getTargetType()) {
                case CANDIDATE -> user.getRole()==Role.CANDIDATE;
                case EMPLOYER -> user.getRole()==Role.EMPLOYER;
                case HOUSEHOLD_EMPLOYER -> user.getRole()==Role.EMPLOYER && household;
                case COMPANY_EMPLOYER -> user.getRole()==Role.EMPLOYER && !household;
            };
            return target && (r.getCompanyType()==null || (type!=null && r.getCompanyType().getId().equals(type.getId())));
        }).toList();
    }
    public Document document(VerificationRecord v) {
        // Legacy evaluator notes were promised private; never publish them retrospectively.
        return new Document(v.getId(),v.getStatus(),v.getOriginalName(),v.getRequirement()==null?"National ID":v.getRequirement().getName(),v.getFileSize(),v.getStoredName()==null?null:v.getReviewerNotes(),v.getSubmittedAt(),v.getReviewedAt(),v.getStoredName()!=null,v.getStoredName()==null);
    }
    public Checklist forUser(User user) {
        var history=records.forUser(user.getId());
        var items=applicable(user).stream().map(r -> new Item(VerificationRequirements.view(r),history.stream().filter(v -> matches(v,r)).findFirst().map(this::document).orElse(null))).toList();
        boolean typeMissing=user.getRole()==Role.EMPLOYER && employers.findByUserId(user.getId()).orElseThrow(VerificationChecklist::missing).getCompanyType()==null;
        var required=items.stream().filter(i -> i.requirement().required()).toList();
        String status;
        if(typeMissing) status="INCOMPLETE";
        else if(required.stream().allMatch(i -> i.submission()!=null && i.submission().status()==VerificationStatus.VERIFIED)) status="VERIFIED";
        else if(required.stream().allMatch(i -> i.submission()==null)) status="NOT_STARTED";
        else if(required.stream().anyMatch(i -> i.submission()!=null && List.of(VerificationStatus.FAILED,VerificationStatus.FLAGGED).contains(i.submission().status()))) status="REJECTED";
        else if(required.stream().anyMatch(i -> i.submission()==null)) status="INCOMPLETE";
        else status="PENDING";
        return new Checklist(status,typeMissing,items,history.stream().map(this::document).toList());
    }
    public String candidateStatus(Long candidateId) { return forUser(candidates.findById(candidateId).orElseThrow(VerificationChecklist::missing).getUser()).status(); }
    public boolean latest(VerificationRecord record) {
        return records.forUser(owner(record).getId()).stream().filter(v -> record.getRequirement()==null
            ? v.getRequirement()==null || "CANDIDATE_NID".equals(v.getRequirement().getCode())
            : matches(v,record.getRequirement())).findFirst().map(v -> v.getId().equals(record.getId())).orElse(false);
    }
    public static ApiException missing() { return new ApiException(404,"NOT_FOUND","Verification record not found."); }
}
