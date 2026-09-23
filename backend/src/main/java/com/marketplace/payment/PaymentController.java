package com.marketplace.payment;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;
    public record EmptyRequest() {}
    @PostMapping("/jobs/{id}/payment") public PaymentView job(@PathVariable Long id, @RequestBody(required=false) EmptyRequest body) { return service.job(id); }
    @PostMapping("/bookings/{id}/payment") public PaymentView booking(@PathVariable Long id, @RequestBody(required=false) EmptyRequest body) { return service.booking(id); }
    @GetMapping("/payments/me") public List<PaymentView> mine() { return service.mine(); }
    @GetMapping("/payments/{id}") public PaymentView get(@PathVariable Long id) { return service.get(id); }
    @PostMapping("/payments/{id}/demo-success") public PaymentView complete(@PathVariable Long id, @RequestBody(required=false) EmptyRequest body) { return service.complete(id); }
    @PostMapping("/payments/{id}/cancel") public PaymentView cancel(@PathVariable Long id, @RequestBody(required=false) EmptyRequest body) { return service.cancel(id); }
}
