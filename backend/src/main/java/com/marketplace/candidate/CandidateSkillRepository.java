package com.marketplace.candidate;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, Long> {
    List<CandidateSkill> findByCandidateId(Long candidateId);
    boolean existsByCandidateIdAndSkillId(Long candidateId, Long skillId);
}

