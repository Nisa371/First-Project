package com.marketplace.job;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
import com.marketplace.candidate.CandidateDtos;
import static com.marketplace.job.JobDtos.*;
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobService service;
    @GetMapping public List<JobView> list() { return service.list(); }
    @GetMapping("/{id}") public JobView get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public JobView create(@Valid @RequestBody JobRequest r) { return service.create(r); }
    @PutMapping("/{id}") public JobView update(@PathVariable Long id, @Valid @RequestBody JobRequest r) { return service.update(id,r); }
    @PostMapping("/{id}/close") public JobView close(@PathVariable Long id) { return service.close(id); }
    @GetMapping("/{id}/shortlist") public List<CandidateDtos.CandidateCard> shortlist(@PathVariable Long id) { return service.shortlist(id); }
    @PostMapping("/{id}/shortlist/{candidateId}") @ResponseStatus(HttpStatus.CREATED)
    public void add(@PathVariable Long id,@PathVariable Long candidateId) { service.add(id,candidateId); }
    @DeleteMapping("/{id}/shortlist/{candidateId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id,@PathVariable Long candidateId) { service.remove(id,candidateId); }
}
