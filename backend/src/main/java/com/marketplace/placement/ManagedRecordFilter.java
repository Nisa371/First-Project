package com.marketplace.placement;

import com.marketplace.candidate.CandidateType;
import com.marketplace.common.api.ApiException;
import com.marketplace.replacement.*;
import com.marketplace.user.*;
import jakarta.persistence.criteria.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;
import java.util.*;

@Data
public class ManagedRecordFilter {
    @Size(max=200) private String candidateName;
    @Size(max=50) private String phone;
    @Size(max=200) private String jobTitle;
    @Size(max=200) private String companyName;
    private CandidateType track;
    @Positive private Long skillId;
    @Positive private Long companyTypeId;
    @Size(max=40) private String status;
    private SlaStatus slaStatus;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate dateFrom;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate dateTo;

    public Specification<Placement> placements(User actor) {
        validate(actor, false);
        return (root, query, cb) -> {
            var predicates=common(root,cb,actor);
            if(status!=null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), PlacementStatus.valueOf(status)));
            if(dateFrom!=null) predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"),dateFrom));
            if(dateTo!=null) predicates.add(cb.lessThanOrEqualTo(root.get("startDate"),dateTo));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
    public Specification<ReplacementRequest> replacements(User actor) {
        validate(actor, true);
        return (root, query, cb) -> {
            var predicates=common(root.join("placement"),cb,actor);
            if(status!=null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), ReplacementStatus.valueOf(status)));
            if(dateFrom!=null) predicates.add(cb.greaterThanOrEqualTo(root.get("requestedAt"),dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant()));
            if(dateTo!=null) predicates.add(cb.lessThan(root.get("requestedAt"),dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
            if(slaStatus!=null) {
                // Unfinished Trade requests can exceed their target before another mutation stores the breach.
                var pending=cb.and(cb.isNotNull(root.get("targetCompletionAt")),root.get("status").in(ReplacementStatus.COMPLETED,ReplacementStatus.CANCELLED,ReplacementStatus.FAILED).not());
                var overdue=cb.and(pending,cb.lessThan(root.get("targetCompletionAt"),Instant.now()));
                predicates.add(slaStatus==SlaStatus.BREACHED ? cb.or(cb.equal(root.get("slaStatus"),slaStatus),overdue)
                    : cb.and(cb.equal(root.get("slaStatus"),slaStatus),cb.not(overdue)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
    private List<Predicate> common(From<?,Placement> placement, CriteriaBuilder cb, User actor) {
        var result=new ArrayList<Predicate>();
        var candidate=placement.join("candidate"); var employer=placement.join("employer");
        if(actor.getRole()==Role.EMPLOYER) result.add(cb.equal(employer.get("user").get("id"),actor.getId()));
        contains(result,cb,candidate.get("fullName"),candidateName);
        contains(result,cb,candidate.get("phone"),phone);
        if(jobTitle!=null && !jobTitle.isBlank()) contains(result,cb,placement.join("job",JoinType.LEFT).get("title"),jobTitle);
        if(track!=null) result.add(cb.equal(candidate.get("candidateType"),track));
        if(skillId!=null) result.add(cb.equal(placement.get("skill").get("id"),skillId));
        if(actor.getRole()==Role.ADMIN) {
            contains(result,cb,employer.get("companyName"),companyName);
            if(companyTypeId!=null) result.add(cb.equal(employer.get("companyType").get("id"),companyTypeId));
        }
        return result;
    }
    private void validate(User actor, boolean replacement) {
        if(actor.getRole()!=Role.ADMIN && actor.getRole()!=Role.EMPLOYER) throw new ApiException(403,"FORBIDDEN","Only employers and administrators can filter managed records.");
        if(dateFrom!=null && dateTo!=null && dateFrom.isAfter(dateTo)) throw new ApiException(400,"INVALID_FILTER","The from date must be on or before the to date.");
        try { if(status!=null && !status.isBlank()) { if(replacement) ReplacementStatus.valueOf(status); else PlacementStatus.valueOf(status); } }
        catch(IllegalArgumentException ex) { throw new ApiException(400,"INVALID_FILTER","Choose a valid status."); }
    }
    private static void contains(List<Predicate> predicates, CriteriaBuilder cb, Expression<String> field, String value) {
        if(value!=null && !value.isBlank()) {
            String escaped=value.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
            predicates.add(cb.like(cb.lower(field),"%"+escaped+"%",'!'));
        }
    }
}
