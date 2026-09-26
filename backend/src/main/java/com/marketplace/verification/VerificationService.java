package com.marketplace.verification;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.*;
import com.marketplace.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.Instant;
import java.util.List;
import static com.marketplace.verification.VerificationDtos.*;

@Service
@RequiredArgsConstructor
@Transactional
public class VerificationService {
    private final CurrentAccount current;
    private final com.marketplace.notification.NotificationService notifications;
    private final CandidateService profiles;
    private final VerificationRecordRepository records;
    private final VerificationChecklist checklist;
    private final com.marketplace.user.UserRepository users;

    @PreAuthorize("hasRole('CANDIDATE')")
    public List<StatusView> history() {
        return records.findByCandidateIdOrderBySubmittedAtDescIdDesc(profiles.own().getId()).stream().map(this::status).toList();
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public StatusView submit(Submission request) {
        throw new ApiException(410,"DOCUMENT_UPLOAD_REQUIRED","Upload your National ID using the verification checklist.");
    }
    @PreAuthorize("hasAnyRole('EVALUATOR','ADMIN')")
    public List<ReviewView> pending() {
        current.requireActive();
        return java.util.stream.Stream.concat(records.findByStatusOrderBySubmittedAtAsc(VerificationStatus.PENDING).stream(),
            records.findByStatusOrderBySubmittedAtAsc(VerificationStatus.IN_REVIEW).stream())
            .sorted(java.util.Comparator.comparing(VerificationRecord::getSubmittedAt).thenComparing(VerificationRecord::getId))
            .filter(v -> v.getCandidate()!=null && v.getStoredName()==null).map(this::reviewView).toList();
    }
    @PreAuthorize("hasAnyRole('EVALUATOR','ADMIN')")
    public ReviewView detail(Long id) {
        current.requireActive(); var v=records.findById(id).orElseThrow(CandidateService::missing); legacyOnly(v); return reviewView(v);
    }
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewView review(Long id, Decision decision) {
        var reviewer = current.requireActive();
        if (!List.of(VerificationStatus.VERIFIED, VerificationStatus.FAILED, VerificationStatus.FLAGGED).contains(decision.status()))
            throw new ApiException(400, "INVALID_DECISION", "Choose VERIFIED, FAILED or FLAGGED.");
        users.findByIdForUpdate(records.ownerId(id).orElseThrow(CandidateService::missing)).orElseThrow(CandidateService::missing);
        var v = records.findByIdForUpdate(id).orElseThrow(CandidateService::missing);
        legacyOnly(v);
        if (!checklist.latest(v) || (v.getStatus() != VerificationStatus.PENDING && v.getStatus() != VerificationStatus.IN_REVIEW))
            throw new ApiException(409, "FINAL_REVIEW", "This verification already has a final decision.");
        v.setReviewerUser(reviewer); v.setStatus(decision.status()); v.setReviewerNotes(decision.notes().trim()); v.setReviewedAt(Instant.now());
        notifications.afterCommit(checklist.owner(v).getId(),"Verification result",
            "Your verification submission #"+v.getId()+" was "+(decision.status()==VerificationStatus.VERIFIED?"approved.":"rejected."));
        return reviewView(v);
    }
    private void legacyOnly(VerificationRecord v) {
        if(v.getCandidate()==null || v.getStoredName()!=null) throw new ApiException(403,"ADMIN_REVIEW_REQUIRED","Document submissions require administrator review.");
    }
    private StatusView status(VerificationRecord v) { return new StatusView(v.getId(),v.getStatus(),v.getSubmittedAt(),v.getReviewedAt()); }
    private ReviewView reviewView(VerificationRecord v) {
        var c=v.getCandidate(); return new ReviewView(v.getId(),c.getFullName(),c.getCandidateType().name(),c.getPrimaryTradeCategory(),
            c.getLocation(),v.getIdentityReference(),v.getStatus(),v.getReviewerNotes(),v.getSubmittedAt(),v.getReviewedAt());
    }
}
