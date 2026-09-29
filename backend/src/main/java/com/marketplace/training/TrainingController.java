package com.marketplace.training;
import com.marketplace.candidate.CandidateType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Set;
@RestController @RequiredArgsConstructor
public class TrainingController {
 private final TrainingService service;
 public record Request(@NotNull @Positive Long candidateId,@NotNull @Positive Long programId) {}
 public record ProgramInput(@NotBlank @Size(max=200) String title,@NotBlank @Size(max=200) String providerName,
     @NotBlank @Size(max=3000) String description,@NotNull Boolean active,@NotEmpty Set<@NotNull @Positive Long> skillIds) {}
 @GetMapping("/api/training-programs") public List<TrainingService.Program> catalog(){return service.catalog();}
 @PostMapping("/api/training-programs") public TrainingService.Program create(@Valid @RequestBody ProgramInput r){return service.save(null,r);}
 @PutMapping("/api/training-programs/{id}") public TrainingService.Program update(@PathVariable Long id,@Valid @RequestBody ProgramInput r){return service.save(id,r);}
 @GetMapping("/api/referrals") public List<TrainingService.ReferralView> list(){return service.list();}
 @GetMapping("/api/referrals/candidates") public List<TrainingService.CandidateView> candidates(
     @RequestParam(defaultValue="") String name,@RequestParam(defaultValue="") String phone,
     @RequestParam(required=false) CandidateType track,@RequestParam(required=false) Long skillId,@RequestParam(defaultValue="0") int page){return service.search(name,phone,track,skillId,page);}
 @PostMapping("/api/referrals") public TrainingService.ReferralView refer(@Valid @RequestBody Request r){return service.refer(r.candidateId(),r.programId());}
}
