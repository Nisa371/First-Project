package com.marketplace.ai;

import com.marketplace.job.JobApplicationSubmitted;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component @lombok.extern.slf4j.Slf4j
public class ApplicationEvaluationListener {
    private final AsyncTaskExecutor executor;
    private final AiCandidateEvaluationService evaluations;
    private final com.marketplace.job.JobApplicationRepository applications;

    public ApplicationEvaluationListener(@Qualifier("applicationTaskExecutor") AsyncTaskExecutor executor,
                                         AiCandidateEvaluationService evaluations, com.marketplace.job.JobApplicationRepository applications) {
        this.applications = applications;
        this.executor = executor;
        this.evaluations = evaluations;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void submitted(JobApplicationSubmitted event) {
        try {
            // Reuse Spring Boot's managed executor; never run Gemini on the request thread.
            executor.execute(() -> {
                try {
                    // The service proxy starts a separate transaction on this worker thread.
                    evaluations.evaluateSubmitted(event.applicationId());
                } catch (RuntimeException e) {
                    // Do not log provider text, private expectations or candidate evidence.
                    log.warn("Automatic evaluation could not complete for application {}", event.applicationId());
                }
            });
        } catch (RuntimeException e) {
            // Dispatch failure must not turn a committed application into an HTTP failure.
            log.warn("Automatic evaluation could not start for application {}", event.applicationId());
        }
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void contentChanged(com.marketplace.candidate.CandidateEvaluationContentChanged event) {
        try {
            executor.execute(() -> {
                try {
                    var component = event.cv() && event.portfolio() ? AiCandidateEvaluationService.Component.BOTH
                        : event.cv() ? AiCandidateEvaluationService.Component.CV : AiCandidateEvaluationService.Component.PORTFOLIO;
                    for (Long id : applications.outstandingEvaluations(event.candidateId(), event.cv(), event.portfolio())) {
                        try {
                            evaluations.evaluateOutstanding(id, component);
                        } catch (RuntimeException ignored) {
                            log.warn("Automatic evaluation could not complete for application {}", id);
                        }
                    }
                } catch (RuntimeException ignored) {
                    log.warn("Automatic evaluation could not load outstanding applications");
                }
            });
        } catch (RuntimeException ignored) {
            log.warn("Automatic evaluation could not start after candidate content update");
        }
    }

}
