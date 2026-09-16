package com.marketplace.booking;

import java.util.List;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findBySlotEvaluatorUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);
    long countBySlotIdAndStatus(Long slotId, BookingStatus status);
    boolean existsBySlotIdAndCandidateIdAndStatus(Long slotId, Long candidateId, BookingStatus status);

    @Query("""
            select count(b) > 0 from Booking b
            where b.candidate.id = :candidateId and b.status = com.marketplace.booking.BookingStatus.BOOKED
              and b.slot.startTime < :endTime and b.slot.endTime > :startTime
            """)
    boolean hasOverlappingBooking(@Param("candidateId") Long candidateId,
            @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}

