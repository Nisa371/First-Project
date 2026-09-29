package com.marketplace.training;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingProgramRepository extends JpaRepository<TrainingProgram, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from TrainingProgram p where p.id = :id")
    java.util.Optional<TrainingProgram> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    List<TrainingProgram> findByActiveTrueOrderByTitleAsc();
}

