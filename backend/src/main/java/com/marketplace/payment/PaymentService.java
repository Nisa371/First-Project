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
    private final com.marketplace.placement.PlacementService placementService;
    private final com.marketplace.replacement.ReplacementRequestRepository replacements;
    private final BookingRepository bookings;
    private final CandidateProfileRepository candidates;
    private final AppointmentSlotRepository slots;

    @PreAuthorize("hasRole('EMPLOYER')")
    public PaymentView job(Long id, java.time.LocalDate endingDate) {
        var u=current.requireActive();
        var j=jobs.findOwnedForUpdate(id,u.getId()).orElseThrow(PaymentService::missing);
        if(!"VERIFIED".equals(verifications.forUser(j.getEmployer().getUser()).status()))
            throw new ApiException(403,"VERIFICATION_REQUIRED","Employer verification is required before posting jobs.");
        if(replacements.findByFreeReplacementJobId(id).isPresent()) throw conflict("This replacement vacancy does not require publication payment.");
        if(j.effectiveStatus()==JobStatus.ACTIVE) throw conflict("This portal is already active.");
        if(j.getEmploymentType()==null) throw conflict("Choose an employment type by editing the job before publication.");
        if(j.effectiveStatus()==JobStatus.CLOSED) j.setEmployerRequestedEndDate(endingDate);
        else if(endingDate!=null) j.setEmployerRequestedEndDate(endingDate);
        Job.closingDate(j.getEmployerRequestedEndDate(),Instant.now());
        var p=payments.findByJobId(id).filter(existing -> existing.getStatus()==PaymentStatus.PENDING).orElseGet(() -> {
            var payment=new PaymentTransaction(); payment.setPayer(u); payment.setJob(j);
            payment.setPurpose(PaymentPurpose.JOB_POSTING); payment.setAmount(prices.jobPostingFee()); return initialize(payment);
        });
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
                if(j.effectiveStatus()==JobStatus.ACTIVE || j.getOriginalJob()!=null) throw conflict("This job cannot be published using this payment.");
                if(j.getEmploymentType()==null) throw conflict("Select an employment type before publication.");
                Job.closingDate(j.getEmployerRequestedEndDate(),Instant.now());
            } else {
                if(b.getStatus()!=BookingStatus.PENDING_PAYMENT) throw conflict("This booking is no longer pending.");
                var s=b.getSlot();
                if(!s.isActive() || !s.getStartTime().isAfter(Instant.now())) throw conflict("This session is no longer available. Cancel and choose another time.");
                if(bookings.countBySlotIdAndStatus(s.getId(),BookingStatus.BOOKED)>=s.getCapacity()) throw conflict("This session is full. Cancel and choose another time.");
                if(bookings.hasOverlappingBooking(b.getCandidate().getId(),s.getStartTime(),s.getEndTime())) throw conflict("You already have a confirmed appointment during this time.");
            }
        }
        p.setStatus(cancel ? PaymentStatus.CANCELLED : PaymentStatus.SUCCESS); p.setCompletedAt(Instant.now());
        if(j!=null && !cancel) {
            if(j.getStatus()==JobStatus.ACTIVE) placementService.releaseUnhiredApplicants(j);
            var now=Instant.now(); j.setActivatedAt(now); j.setPortalClosesAt(Job.closingDate(j.getEmployerRequestedEndDate(),now)); j.setStatus(JobStatus.ACTIVE);
        }
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
            p.getJob()!=null?p.getJob().getTitle():p.getBooking().getPurpose()+" · "+p.getBooking().getSlot().getStartTime(),
            p.getJob()==null?null:(p.getStatus()==PaymentStatus.SUCCESS?p.getJob().closingTime():previewClosing(p.getJob())),p.getJob()!=null && p.getJob().getEmployerRequestedEndDate()==null);
    }
    private Instant previewClosing(Job j) {
        return j.getEmployerRequestedEndDate()==null?Instant.now().atZone(java.time.ZoneOffset.UTC).plusMonths(1).toInstant():j.getEmployerRequestedEndDate().atStartOfDay(java.time.ZoneId.of("Asia/Dhaka")).toInstant();
    }
    private static ApiException missing() { return new ApiException(404,"NOT_FOUND","Payment or resource not found."); }
    private static ApiException conflict(String message) { return new ApiException(409,"PAYMENT_CONFLICT",message); }
}
