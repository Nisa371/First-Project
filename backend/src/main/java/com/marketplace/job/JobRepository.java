package com.marketplace.job;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select j from Job j where j.id = :id and j.employer.user.id = :userId")
    Optional<Job> findOwnedForUpdate(@org.springframework.data.repository.query.Param("id") Long id,
        @org.springframework.data.repository.query.Param("userId") Long userId);
    List<Job> findByEmployerIdOrderByCreatedAtDesc(Long employerId);
    Optional<Job> findByIdAndEmployerUserId(Long id, Long userId);
    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);
}

