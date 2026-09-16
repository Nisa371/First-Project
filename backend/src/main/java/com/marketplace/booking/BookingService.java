package com.marketplace.booking;
import java.time.Instant;
import java.util.List;
import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.*;
import com.marketplace.user.UserRepository;
import com.marketplace.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import static com.marketplace.booking.BookingDtos.*;
@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {
    private final CurrentAccount current;
    private final CandidateProfileRepository candidates;
    private final UserRepository users;
    private final AppointmentSlotRepository slots;
    private final BookingRepository bookings;
    private CandidateProfile candidate() { return candidates.findByUserId(current.requireActive().getId()).orElseThrow(BookingService::missing); }
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<SlotView> available() { current.requireActive(); return slots.findByActiveTrueAndStartTimeAfterOrderByStartTimeAsc(Instant.now()).stream().map(this::slotView).toList(); }
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<BookingView> mine() { return bookings.findByCandidateIdOrderByCreatedAtDesc(candidate().getId()).stream().map(this::view).toList(); }
    @PreAuthorize("hasRole('CANDIDATE')")
    public BookingView book(BookingRequest r) {
        var c=candidate();
        // Candidate lock serializes bookings across different slots; slot lock protects shared capacity.
        candidates.findByIdForUpdate(c.getId()).orElseThrow(BookingService::missing);
        var s=slots.findByIdForUpdate(r.slotId()).orElseThrow(BookingService::missing);
        if(!s.isActive() || !s.getStartTime().isAfter(Instant.now())) throw conflict("This slot is no longer available.");
        if(bookings.hasOverlappingBooking(c.getId(),s.getStartTime(),s.getEndTime())) throw conflict("You already have an appointment during this time.");
        if(bookings.countBySlotIdAndStatus(s.getId(),BookingStatus.BOOKED)>=s.getCapacity()) throw conflict("This slot is full. Please choose another time.");
        var b=new Booking(); b.setCandidate(c); b.setSlot(s); b.setPurpose(r.purpose()); b.setNotes(r.notes());
        return view(bookings.saveAndFlush(b));
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public BookingView cancel(Long id) {
        var c=candidate(); candidates.findByIdForUpdate(c.getId()).orElseThrow(BookingService::missing);
        var b=bookings.findById(id).orElseThrow(BookingService::missing);
        if(!b.getCandidate().getId().equals(c.getId())) throw missing();
        slots.findByIdForUpdate(b.getSlot().getId()).orElseThrow(BookingService::missing);
        if(b.getStatus()==BookingStatus.CANCELLED) return view(b);
        if(b.getStatus()!=BookingStatus.BOOKED || !b.getSlot().getStartTime().isAfter(Instant.now())) throw conflict("Only upcoming bookings can be cancelled.");
        b.setStatus(BookingStatus.CANCELLED); bookings.flush(); return view(b);
    }
    @PreAuthorize("hasRole('EVALUATOR')")
    public List<SlotView> ownSlots() { return slots.findByEvaluatorUserIdOrderByStartTimeAsc(current.requireActive().getId()).stream().map(this::slotView).toList(); }
    @PreAuthorize("hasRole('EVALUATOR')")
    public List<BookingView> appointments() { return bookings.findBySlotEvaluatorUserIdOrderByCreatedAtDesc(current.requireActive().getId()).stream().map(this::view).toList(); }
    @PreAuthorize("hasRole('EVALUATOR')")
    public SlotView create(SlotRequest r) {
        // Normalize precision before comparison and persistence, so adjacent slots remain adjacent.
        var start=r.startTime().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        var end=r.endTime().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        var u=current.requireActive(); users.findByIdForUpdate(u.getId()).orElseThrow(BookingService::missing);
        if(!start.isAfter(Instant.now()) || !end.isAfter(start)) throw new ApiException(400,"INVALID_TIME","Choose a future start and a later end time.");
        if(slots.existsByEvaluatorUserIdAndActiveTrueAndStartTimeLessThanAndEndTimeGreaterThan(u.getId(),end,start)) throw conflict("This overlaps one of your existing slots.");
        var s=new AppointmentSlot(); s.setEvaluatorUser(u); s.setStartTime(start); s.setEndTime(end); s.setCapacity(r.capacity());
        return slotView(slots.saveAndFlush(s));
    }
    @PreAuthorize("hasRole('EVALUATOR')")
    public SlotView close(Long id) {
        var s=slots.findByIdForUpdate(id).orElseThrow(BookingService::missing);
        if(s.getEvaluatorUser()==null || !s.getEvaluatorUser().getId().equals(current.requireActive().getId())) throw missing();
        if(bookings.countBySlotIdAndStatus(id,BookingStatus.BOOKED)>0) throw conflict("A slot with bookings cannot be closed.");
        s.setActive(false); return slotView(s);
    }
    private SlotView slotView(AppointmentSlot s) { return new SlotView(s.getId(),s.getStartTime(),s.getEndTime(),s.getCapacity(),
        Math.max(0,s.getCapacity()-bookings.countBySlotIdAndStatus(s.getId(),BookingStatus.BOOKED)),s.isActive()); }
    private BookingView view(Booking b) { return new BookingView(b.getId(),slotView(b.getSlot()),b.getCandidate().getFullName(),b.getPurpose(),b.getStatus(),b.getNotes()); }
    private static ApiException missing() { return new ApiException(404,"NOT_FOUND","Appointment not found."); }
    private static ApiException conflict(String m) { return new ApiException(409,"BOOKING_CONFLICT",m); }
}
