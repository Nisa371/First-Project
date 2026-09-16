package com.marketplace.booking;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
import static com.marketplace.booking.BookingDtos.*;
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService service;
    @GetMapping("/appointment-slots") public List<SlotView> slots() { return service.available(); }
    @PostMapping("/bookings") @ResponseStatus(HttpStatus.CREATED) public BookingView book(@Valid @RequestBody BookingRequest r) { return service.book(r); }
    @GetMapping("/bookings/me") public List<BookingView> mine() { return service.mine(); }
    @PostMapping("/bookings/{id}/cancel") public BookingView cancel(@PathVariable Long id) { return service.cancel(id); }
    @GetMapping("/evaluator/appointment-slots") public List<SlotView> ownSlots() { return service.ownSlots(); }
    @PostMapping("/evaluator/appointment-slots") @ResponseStatus(HttpStatus.CREATED) public SlotView create(@Valid @RequestBody SlotRequest r) { return service.create(r); }
    @PostMapping("/evaluator/appointment-slots/{id}/close") public SlotView close(@PathVariable Long id) { return service.close(id); }
    @GetMapping("/evaluator/bookings") public List<BookingView> appointments() { return service.appointments(); }
}
