package com.marketplace.candidate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import static com.marketplace.candidate.StructuredCvDtos.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/candidates")
public class StructuredCvController {
    private final StructuredCvService service;
    @GetMapping("/me/built-cv") public View own() { return service.own(); }
    @PutMapping("/me/built-cv") public View save(@Valid @RequestBody Content request) { return service.save(request); }
    @GetMapping("/{id}/built-cv") public View applicant(@PathVariable Long id) { return service.applicant(id); }
}
