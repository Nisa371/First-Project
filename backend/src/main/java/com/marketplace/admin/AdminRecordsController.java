package com.marketplace.admin;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/records") @RequiredArgsConstructor
public class AdminRecordsController {
    private final AdminRecordsService service;
    @GetMapping("/{section}") public AdminRecordsService.Page list(@PathVariable String section,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(section,search,status,page,size); }
    @GetMapping("/{section}/{id}") public AdminRecordsService.Row get(@PathVariable String section,@PathVariable Long id) { return service.get(section,id); }
}
