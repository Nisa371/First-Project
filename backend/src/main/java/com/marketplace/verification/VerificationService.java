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
    private final CandidateService profiles;
    private final CandidateProfileRepository candidates;
    private final VerificationRecordRepository records;

    @PreAuthorize("hasRole('CANDIDATE')")
    public List<StatusView> history() {
        return records.findByCandidateIdOrderBySubmittedAtDescIdDesc(profiles.own().getId()).stream().map(this::status).toList();
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public StatusView submit(Submission request) {
        var c = candidates.findByIdForUpdate(profiles.own().getId()).orElseThrow(CandidateService::missing);
        var latest = records.findFirstByCandidateIdOrderBySubmittedAtDescIdDesc(c.getId());
        if (latest.isPresent() && latest.get().getStatus() != VerificationStatus.FAILED && latest.get().getStatus() != VerificationStatus.FLAGGED)
            throw new ApiException(409, "VERIFICATION_EXISTS", "Your verification is pending or already verified.");
        var record = new VerificationRecord(); record.setCandidate(c);
        record.setIdentityReference(request.identityReference().trim());
        return status(records.saveAndFlush(record));
    }
    @PreAuthorize("hasRole('EVALUATOR')")
    public List<ReviewView> pending() {
        current.requireActive();
        return java.util.stream.Stream.concat(records.findByStatusOrderBySubmittedAtAsc(VerificationStatus.PENDING).stream(),
            records.findByStatusOrderBySubmittedAtAsc(VerificationStatus.IN_REVIEW).stream())
            .sorted(java.util.Comparator.comparing(VerificationRecord::getSubmittedAt).thenComparing(VerificationRecord::getId))
            .map(this::reviewView).toList();
    }
    @PreAuthorize("hasRole('EVALUATOR')")
    public ReviewView detail(Long id) {
        current.requireActive(); return reviewView(records.findById(id).orElseThrow(CandidateService::missing));
    }
    @PreAuthorize("hasRole('EVALUATOR')")
    public ReviewView review(Long id, Decision decision) {
        var reviewer = current.requireActive();
        if (!List.of(VerificationStatus.VERIFIED, VerificationStatus.FAILED, VerificationStatus.FLAGGED).contains(decision.status()))
            throw new ApiException(400, "INVALID_DECISION", "Choose VERIFIED, FAILED or FLAGGED.");
        var v = records.findByIdForUpdate(id).orElseThrow(CandidateService::missing);
        if (v.getStatus() != VerificationStatus.PENDING && v.getStatus() != VerificationStatus.IN_REVIEW)
            throw new ApiException(409, "FINAL_REVIEW", "This verification already has a final decision.");
        v.setReviewerUser(reviewer); v.setStatus(decision.status()); v.setReviewerNotes(decision.notes().trim()); v.setReviewedAt(Instant.now());
        return reviewView(v);
    }
    private StatusView status(VerificationRecord v) { return new StatusView(v.getId(),v.getStatus(),v.getSubmittedAt(),v.getReviewedAt()); }
    private ReviewView reviewView(VerificationRecord v) {
        var c=v.getCandidate(); return new ReviewView(v.getId(),c.getFullName(),c.getCandidateType().name(),c.getPrimaryTradeCategory(),
            c.getLocation(),v.getIdentityReference(),v.getStatus(),v.getReviewerNotes(),v.getSubmittedAt(),v.getReviewedAt());
    }
}
