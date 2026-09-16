package com.marketplace.assessment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.marketplace.candidate.CandidateType;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByCandidateTypeAndActiveTrue(CandidateType candidateType);
}

