package com.marketplace.employer;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/employers/me")
@RequiredArgsConstructor
public class EmployerController {
    private final EmployerService service;
    @GetMapping public EmployerService.Profile own() { return service.profile(); }
    @PutMapping public EmployerService.Profile update(@Valid @RequestBody EmployerService.Update r) { return service.update(r); }
}
