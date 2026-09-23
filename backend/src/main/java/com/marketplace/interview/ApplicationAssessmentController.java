package com.marketplace.interview;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
import static com.marketplace.interview.AssessmentDtos.*;

@RestController @lombok.RequiredArgsConstructor
public class ApplicationAssessmentController {
    private final ApplicationAssessmentService service;
    @GetMapping("/api/candidate/applications/{id}/assessment")
    public SessionView get(@PathVariable Long id) { return service.get(id); }
    @PostMapping("/api/candidate/applications/{id}/assessment/start")
    public SessionView start(@PathVariable Long id, @RequestBody(required=false) String body) { empty(body); return service.start(id); }
    @PostMapping("/api/candidate/assessment/{id}/messages")
    public SessionView answer(@PathVariable Long id, @RequestParam @Min(0) int expectedTurn, @Valid @RequestBody Answer answer) {
        return service.answer(id,expectedTurn,answer);
    }
    @GetMapping("/api/employer/applications/{id}/assessment")
    public EmployerView review(@PathVariable Long id) { return service.review(id); }
    @PostMapping("/api/employer/applications/{id}/assessment/retry-evaluation")
    public EmployerView retry(@PathVariable Long id, @RequestBody(required=false) String body) { empty(body); return service.retry(id); }
    private void empty(String body) {
        if (body!=null && !body.isBlank()) throw new com.marketplace.common.api.ApiException(400,"INVALID_ASSESSMENT_REQUEST","This action does not accept request data.");
    }
}
