package com.marketplace.placement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReplacementGuaranteePolicyController {
    private final ReplacementGuaranteePolicyService service;

    @GetMapping({"/api/admin/replacement-guarantee", "/api/placements/replacement-guarantee"})
    public ReplacementGuaranteePolicyService.Policy read() { return service.read(); }

    @PutMapping("/api/admin/replacement-guarantee")
    public ReplacementGuaranteePolicyService.Policy update(@Valid @RequestBody ReplacementGuaranteePolicyService.Policy request) {
        return service.update(request);
    }
}
