package com.marketplace.skill;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<Skill> findFirstByOrderByIdAsc();
    @org.springframework.data.jpa.repository.Query("select s from Skill s where s.active=true order by lower(s.name), s.id")
    List<Skill> findByActiveTrueOrderByNameAsc();
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Skill s set s.category=upper(s.category) where lower(s.category) in ('tech','trade')")
    void normalizeCategories();
    Optional<Skill> findByNameIgnoreCase(String name);
}

