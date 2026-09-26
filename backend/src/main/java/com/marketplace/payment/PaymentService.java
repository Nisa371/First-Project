package com.marketplace.payment;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.booking.*;
import com.marketplace.candidate.CandidateProfileRepository;
import com.marketplace.job.*;
import com.marketplace.common.api.ApiException;
import com.marketplace.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
@PreAuthorize("hasAnyRole('EMPLOYER','CANDIDATE')")
public class PaymentService {
    private final CurrentAccount current;
    private final com.marketplace.verification.VerificationChecklist verifications;
    private final PaymentRepository payments;
    private final DemoPaymentPrices prices;
    private final JobRepository jobs;
    private final BookingRepository bookings;
    private final CandidateProfileRepository candidates;
    private final AppointmentSlotRepository slots;

    @PreAuthorize("hasRole('EMPLOYER')")
    public PaymentView job(Long id) {
        var u=current.requireActive();
        var j=jobs.findOwnedForUpdate(id,u.getId()).orElseThrow(PaymentService::missing);
        if(!"VERIFIED".equals(verifications.forUser(j.getEmployer().getUser()).status()))
            throw new ApiException(403,"VERIFICATION_REQUIRED","Employer verification is required before posting jobs.");
        if(j.getStatus()!=JobStatus.DRAFT) throw conflict("Only unpaid drafts need a posting payment.");
        var p=payments.findByJobId(id).orElseGet(() -> {
            var payment=new PaymentTransaction(); payment.setPayer(u); payment.setJob(j);
            payment.setPurpose(PaymentPurpose.JOB_POSTING); payment.setAmount(prices.jobPostingFee()); return initialize(payment);
        });
        if(p.getStatus()==PaymentStatus.CANCELLED) { p.setStatus(PaymentStatus.PENDING); p.setCompletedAt(null); }
        return view(p);
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public PaymentView booking(Long id) {
        var b=lockBooking(id);
        if(b.getStatus()!=BookingStatus.PENDING_PAYMENT) throw conflict("Only pending bookings need payment.");
        var p=payments.findByBookingId(id).orElseGet(() -> {
            var payment=new PaymentTransaction(); payment.setPayer(current.requireActive()); payment.setBooking(b);
            payment.setPurpose(PaymentPurpose.SESSION_BOOKING); payment.setAmount(prices.sessionBookingFee()); return initialize(payment);
        });
        return view(p);
    }
    private PaymentTransaction initialize(PaymentTransaction p) {
        p.setCurrency(prices.currency()); p.setReference("DEMO-PAY-"+UUID.randomUUID());
        return payments.saveAndFlush(p);
    }
    public PaymentView get(Long id) { return view(owned(id)); }
    public List<PaymentView> mine() { return payments.findByPayerIdOrderByCreatedAtDesc(current.requireActive().getId()).stream().map(this::view).toList(); }
    public PaymentView complete(Long id) { return finish(id,false); }
    public PaymentView cancel(Long id) { return finish(id,true); }
    private PaymentView finish(Long id,boolean cancel) {
        var p=owned(id);
        // Lock resources in the same order as their existing mutation services, then refresh payment.
        Job j=null; Booking b=null;
        if(p.getPurpose()==PaymentPurpose.JOB_POSTING) {
            if(current.requireActive().getRole()!=Role.EMPLOYER) throw missing();
            j=jobs.findOwnedForUpdate(p.getJob().getId(),current.requireActive().getId()).orElseThrow(PaymentService::missing);
        } else {
            if(current.requireActive().getRole()!=Role.CANDIDATE) throw missing();
            b=lockBooking(p.getBooking().getId());
        }
        entityManager.refresh(p, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(p.getStatus()==PaymentStatus.SUCCESS || (cancel && p.getStatus()==PaymentStatus.CANCELLED)) return view(p);
        if(p.getStatus()!=PaymentStatus.PENDING) throw conflict("This payment is no longer pending.");
        if(!cancel) {
            if(j!=null) {
                if(!"VERIFIED".equals(verifications.forUser(j.getEmployer().getUser()).status()))
                    throw new ApiException(403,"VERIFICATION_REQUIRED","Employer verification is required before posting jobs.");
                if(j.getStatus()!=JobStatus.DRAFT) throw conflict("This job is no longer an unpaid draft.");
            } else {
                if(b.getStatus()!=BookingStatus.PENDING_PAYMENT) throw conflict("This booking is no longer pending.");
                var s=b.getSlot();
                if(!s.isActive() || !s.getStartTime().isAfter(Instant.now())) throw conflict("This session is no longer available. Cancel and choose another time.");
                if(bookings.countBySlotIdAndStatus(s.getId(),BookingStatus.BOOKED)>=s.getCapacity()) throw conflict("This session is full. Cancel and choose another time.");
                if(bookings.hasOverlappingBooking(b.getCandidate().getId(),s.getStartTime(),s.getEndTime())) throw conflict("You already have a confirmed appointment during this time.");
            }
        }
        p.setStatus(cancel ? PaymentStatus.CANCELLED : PaymentStatus.SUCCESS); p.setCompletedAt(Instant.now());
        if(j!=null && !cancel) j.setStatus(JobStatus.ACTIVE);
        if(b!=null && b.getStatus()==BookingStatus.PENDING_PAYMENT) b.setStatus(cancel ? BookingStatus.CANCELLED : BookingStatus.BOOKED);
        return view(p);
    }
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private Booking lockBooking(Long id) {
        var c=candidates.findByUserId(current.requireActive().getId()).orElseThrow(PaymentService::missing);
        candidates.findByIdForUpdate(c.getId()).orElseThrow(PaymentService::missing);
        var b=bookings.findById(id).filter(x -> x.getCandidate().getId().equals(c.getId())).orElseThrow(PaymentService::missing);
        slots.findByIdForUpdate(b.getSlot().getId()).orElseThrow(PaymentService::missing);
        entityManager.refresh(b, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return b;
    }
    private PaymentTransaction owned(Long id) { return payments.findByIdAndPayerId(id,current.requireActive().getId()).orElseThrow(PaymentService::missing); }
    private PaymentView view(PaymentTransaction p) {
        return new PaymentView(p.getId(),p.getReference(),p.getPurpose(),p.getAmount(),p.getCurrency(),p.getStatus(),p.getCreatedAt(),p.getCompletedAt(),
            p.getJob()==null?null:p.getJob().getId(),p.getBooking()==null?null:p.getBooking().getId(),
            p.getJob()!=null?p.getJob().getTitle():p.getBooking().getPurpose()+" · "+p.getBooking().getSlot().getStartTime());
    }
    private static ApiException missing() { return new ApiException(404,"NOT_FOUND","Payment or resource not found."); }
    private static ApiException conflict(String message) { return new ApiException(409,"PAYMENT_CONFLICT",message); }
}
