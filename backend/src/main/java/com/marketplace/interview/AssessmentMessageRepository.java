package com.marketplace.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssessmentMessageRepository extends JpaRepository<AssessmentMessage, Long> {
    List<AssessmentMessage> findByAssessmentSessionIdOrderBySequenceNumberAsc(Long sessionId);
}
