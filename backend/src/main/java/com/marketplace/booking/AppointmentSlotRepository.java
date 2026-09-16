package com.marketplace.booking;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {
    List<AppointmentSlot> findByEvaluatorUserIdOrderByStartTimeAsc(Long userId);
    boolean existsByEvaluatorUserIdAndActiveTrueAndStartTimeLessThanAndEndTimeGreaterThan(Long userId, Instant end, Instant start);
    List<AppointmentSlot> findByActiveTrueAndStartTimeAfterOrderByStartTimeAsc(Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AppointmentSlot s where s.id = :id")
    Optional<AppointmentSlot> findByIdForUpdate(@Param("id") Long id);
}

