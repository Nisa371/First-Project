package com.marketplace.ai;

import com.marketplace.job.JobDtos;
import com.marketplace.common.api.ApiException;
import org.springframework.web.bind.annotation.*;

@RestController @lombok.RequiredArgsConstructor
@RequestMapping("/api/jobs/{jobId}/applications/{id}")
public class AiEvaluationController {
    private final AiCandidateEvaluationService service;
    @PostMapping("/evaluate-cv")
    public JobDtos.Applicant cv(@PathVariable Long jobId, @PathVariable Long id, @RequestBody(required=false) String body) {
        empty(body); return service.evaluate(jobId, id, AiCandidateEvaluationService.Component.CV);
    }
    @PostMapping("/evaluate-portfolio")
    public JobDtos.Applicant portfolio(@PathVariable Long jobId, @PathVariable Long id, @RequestBody(required=false) String body) {
        empty(body); return service.evaluate(jobId, id, AiCandidateEvaluationService.Component.PORTFOLIO);
    }
    @PostMapping("/evaluate")
    public JobDtos.Applicant both(@PathVariable Long jobId, @PathVariable Long id, @RequestBody(required=false) String body) {
        empty(body); return service.evaluate(jobId, id, AiCandidateEvaluationService.Component.BOTH);
    }
    private void empty(String body) {
        if (body != null && !body.isBlank()) throw new ApiException(400, "INVALID_EVALUATION_REQUEST", "Evaluation actions do not accept scores or request data.");
    }
}
