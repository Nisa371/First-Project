package com.marketplace.admin;
import com.marketplace.candidate.*;
import com.marketplace.employer.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
public class AdminProfilesController {
    private final CandidateService candidates;
    private final EmployerService employers;
    @PutMapping("/candidates/{id}") public CandidateDtos.ProfileView candidate(@PathVariable Long id,@Valid @RequestBody CandidateDtos.ProfileRequest input) { return candidates.adminUpdate(id,input); }
    @PutMapping("/employers/{id}") public EmployerService.Profile employer(@PathVariable Long id,@Valid @RequestBody EmployerService.Update input) { return employers.adminUpdate(id,input); }
}
