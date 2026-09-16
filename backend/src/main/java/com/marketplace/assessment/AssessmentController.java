package com.marketplace.assessment;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.marketplace.assessment.AssessmentDtos.*;
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssessmentController {
    private final AssessmentService service;
    @GetMapping("/assessments") public List<AssessmentView> list() { return service.list(); }
    @GetMapping("/assessments/{id}") public AssessmentView get(@PathVariable Long id) { return service.get(id); }
    @PostMapping("/assessments/{id}/attempts") public AttemptView start(@PathVariable Long id) { return service.start(id); }
    @GetMapping("/assessment-attempts/me") public List<AttemptView> mine() { return service.mine(); }
    @GetMapping("/assessment-attempts/{id}") public AttemptView detail(@PathVariable Long id) { return service.detail(id); }
    @PutMapping("/assessment-attempts/{id}/answers") public AttemptView save(@PathVariable Long id,@Valid @RequestBody AnswersRequest r) { return service.save(id,r); }
    @PostMapping("/assessment-attempts/{id}/submit") public AttemptView submit(@PathVariable Long id) { return service.submit(id); }
    @GetMapping("/evaluator/attempts") public List<ReviewView> queue() { return service.queue(); }
    @GetMapping("/evaluator/attempts/{id}") public ReviewView review(@PathVariable Long id) { return service.review(id); }
    @PutMapping("/evaluator/attempts/{id}/evaluate") public ReviewView evaluate(@PathVariable Long id,@Valid @RequestBody EvaluationRequest r) { return service.evaluate(id,r); }
    @PostMapping("/evaluator/attempts/{id}/release") public ReviewView release(@PathVariable Long id) { return service.release(id); }
}
