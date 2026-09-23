package com.marketplace.verification;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VerificationRequirementRepository extends JpaRepository<VerificationRequirement,Long> {
    List<VerificationRequirement> findAllByOrderByIdAsc();
    List<VerificationRequirement> findByActiveTrueOrderByIdAsc();
    Optional<VerificationRequirement> findByCode(String code);
    boolean existsByCompanyTypeId(Long id);
}
