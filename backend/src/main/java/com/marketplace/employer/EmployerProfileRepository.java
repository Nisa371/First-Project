package com.marketplace.employer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployerProfileRepository extends JpaRepository<EmployerProfile, Long> {
    boolean existsByCompanyTypeId(Long companyTypeId);
    java.util.List<EmployerProfile> findByCompanyTypeIsNull();
    Optional<EmployerProfile> findByUserId(Long userId);
}

