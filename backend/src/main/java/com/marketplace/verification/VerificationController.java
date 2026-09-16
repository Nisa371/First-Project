package com.marketplace.verification;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
import static com.marketplace.verification.VerificationDtos.*;

@RestController
@RequiredArgsConstructor
public class VerificationController {
    private final VerificationService service;
    @GetMapping("/api/verifications/me") public List<StatusView> history() { return service.history(); }
    @PostMapping("/api/verifications/me") @ResponseStatus(HttpStatus.CREATED)
    public StatusView submit(@Valid @RequestBody Submission r) { return service.submit(r); }
    @GetMapping("/api/evaluator/verifications") public List<ReviewView> pending() { return service.pending(); }
    @GetMapping("/api/evaluator/verifications/{id}") public ReviewView detail(@PathVariable Long id) { return service.detail(id); }
    @PostMapping("/api/evaluator/verifications/{id}/review")
    public ReviewView review(@PathVariable Long id, @Valid @RequestBody Decision r) { return service.review(id,r); }
}
