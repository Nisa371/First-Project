package com.marketplace.companytype;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface CompanyTypeRepository extends JpaRepository<CompanyType, Long> {
    List<CompanyType> findAllByOrderByNameAsc();
    List<CompanyType> findByActiveTrueOrderByNameAsc();
    Optional<CompanyType> findByCode(String code);
    Optional<CompanyType> findByNormalizedName(String name);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CompanyType c where c.id = :id")
    Optional<CompanyType> lockById(Long id);
}
