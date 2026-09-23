package com.marketplace.companytype;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor
public class CompanyTypeController {
    private final CompanyTypeService service;
    @GetMapping("/api/company-types") public List<CompanyTypeService.View> selectable() { return service.selectable(); }
    @GetMapping("/api/admin/company-types") public List<CompanyTypeService.View> all() { return service.all(); }
    @PostMapping("/api/admin/company-types") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public CompanyTypeService.View create(@Valid @RequestBody CompanyTypeService.Input input) { return service.create(input); }
    @PutMapping("/api/admin/company-types/{id}")
    public CompanyTypeService.View update(@PathVariable Long id, @Valid @RequestBody CompanyTypeService.Input input) { return service.update(id, input); }
    @DeleteMapping("/api/admin/company-types/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) { service.remove(id); }
}
