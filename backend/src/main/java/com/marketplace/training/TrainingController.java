package com.marketplace.training;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
@RestController @RequiredArgsConstructor
public class TrainingController {
 private final TrainingService service;
 public record Request(@NotNull @Positive Long evaluationId,@NotNull @Positive Long programId) {}
 @GetMapping("/api/training-programs") public List<TrainingService.Program> catalog(){return service.catalog();}
 @GetMapping("/api/referrals") public List<TrainingService.ReferralView> list(){return service.list();}
 @GetMapping("/api/referrals/eligible") public List<TrainingService.Eligible> eligible(){return service.eligible();}
 @PostMapping("/api/referrals") public TrainingService.ReferralView refer(@Valid @RequestBody Request r){return service.refer(r.evaluationId(),r.programId());}
}
