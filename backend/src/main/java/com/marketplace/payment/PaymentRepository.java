package com.marketplace.payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PaymentRepository extends JpaRepository<PaymentTransaction,Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentTransaction> findFirstByJobIdOrderByIdDesc(Long id);
    default Optional<PaymentTransaction> findByJobId(Long id) { return findFirstByJobIdOrderByIdDesc(id); }
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentTransaction> findByBookingId(Long id);
    Optional<PaymentTransaction> findFirstByJobIdAndStatusOrderByCompletedAtDesc(Long id, PaymentStatus status);
    Optional<PaymentTransaction> findByIdAndPayerId(Long id,Long payerId);
    List<PaymentTransaction> findByPayerIdOrderByCreatedAtDesc(Long payerId);
}
