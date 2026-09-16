package com.marketplace.admin;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.marketplace.user.AccountStatus;
import java.util.List;
@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
public class AdminController {
 private final AdminService service;
 public record StatusRequest(@NotNull AccountStatus status) {}
 @GetMapping("/stats") public AdminService.Stats stats(){return service.stats();}
 @GetMapping("/users") public List<AdminService.Account> users(){return service.users();}
 @PatchMapping("/users/{id}/status") public AdminService.Account status(@PathVariable Long id,@Valid @RequestBody StatusRequest r){return service.status(id,r.status());}
 @GetMapping("/jobs") public List<AdminService.JobView> jobs(){return service.jobs();}
 @GetMapping("/verifications") public List<AdminService.Verification> verifications(){return service.verifications();}
 @GetMapping("/skills") public List<AdminService.SkillView> skills(){return service.skills();}
}
