package com.marketplace.skill;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<Skill> findFirstByOrderByIdAsc();
    List<Skill> findByActiveTrueOrderByNameAsc();
    Optional<Skill> findByNameIgnoreCase(String name);
}

