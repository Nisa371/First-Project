package com.marketplace.interview;

import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Component @EnableScheduling @lombok.RequiredArgsConstructor @lombok.extern.slf4j.Slf4j
public class AssessmentExpiry {
    private final AssessmentSessionRepository sessions;
    private final ApplicationAssessmentService assessments;

    @Scheduled(fixedDelay=1000)
    public void expire() {
        for (Long id : sessions.expiredApplications(AssessmentSession.Status.IN_PROGRESS,Instant.now().minusSeconds(300))) {
            try { assessments.expireApplication(id); }
            catch (RuntimeException e) { log.warn("Could not finalize expired assessment for application {}",id); }
        }
    }
}
