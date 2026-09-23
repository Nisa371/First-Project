package com.marketplace.admin;
import com.marketplace.skill.AdminSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/skills") @RequiredArgsConstructor
public class AdminSkillsController {
    private final AdminSkillService service;
    @PostMapping public void create(@Valid @RequestBody AdminSkillService.Input input) { service.save(null,input); }
    @PutMapping("/{id}") public void update(@PathVariable Long id,@Valid @RequestBody AdminSkillService.Input input) { service.save(id,input); }
}
