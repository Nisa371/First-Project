package com.marketplace.candidate;

import com.marketplace.replacement.QueueEligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReadinessController {
    private final CandidateService profiles;
    private final QueueEligibilityService eligibility;
    @GetMapping("/api/candidates/me/readiness")
    public QueueEligibilityService.Readiness own() { return eligibility.check(profiles.own().getId(),null); }
}
