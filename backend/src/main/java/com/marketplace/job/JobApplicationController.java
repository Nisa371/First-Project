package com.marketplace.job;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
import static com.marketplace.job.JobDtos.*;
@RestController @RequiredArgsConstructor
public class JobApplicationController {
    private final JobApplicationService service;
    @GetMapping("/api/jobs/{jobId}/evaluation-weights")
    public EvaluationWeights weights(@PathVariable Long jobId) { return service.weights(jobId); }
    @PutMapping("/api/jobs/{jobId}/evaluation-weights")
    public EvaluationWeights weights(@PathVariable Long jobId, @Valid @RequestBody EvaluationWeights input) {
        return service.updateWeights(jobId,input);
    }
    @GetMapping("/api/candidates/me/jobs") public JobPage openings(@Valid @ModelAttribute JobSearch search) { return service.openings(search); }
    @GetMapping("/api/candidates/me/jobs/{id}") public PublicJob opening(@PathVariable Long id) { return service.opening(id); }
    @PostMapping("/api/jobs/{id}/apply") @ResponseStatus(HttpStatus.CREATED)
    public ApplicationView apply(@PathVariable Long id) { return service.apply(id); }
    @GetMapping("/api/candidates/me/applications") public List<ApplicationView> mine() { return service.mine(); }
    @PatchMapping("/api/applications/{id}/withdraw") public ApplicationView withdraw(@PathVariable Long id) { return service.withdraw(id); }
    @GetMapping("/api/jobs/{jobId}/applications") public List<Applicant> applicants(@PathVariable Long jobId) { return service.applicants(jobId); }
    @GetMapping("/api/jobs/{jobId}/applications/{id}") public Applicant applicant(@PathVariable Long jobId,@PathVariable Long id) { return service.applicant(jobId,id); }
    @PatchMapping("/api/jobs/{jobId}/applications/{id}/status")
    public Applicant status(@PathVariable Long jobId,@PathVariable Long id,@Valid @RequestBody StatusRequest input) { return service.status(jobId,id,input.status()); }
}
