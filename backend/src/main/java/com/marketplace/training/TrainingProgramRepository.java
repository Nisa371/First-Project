package com.marketplace.training;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingProgramRepository extends JpaRepository<TrainingProgram, Long> {
    List<TrainingProgram> findByActiveTrueOrderByTitleAsc();
}

