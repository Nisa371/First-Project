package com.marketplace.candidate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<CandidateProfile> {
    interface TrainingCandidate {
        Long getId(); String getFullName(); String getPhone(); CandidateType getCandidateType();
        String getLocation(); Availability getAvailability();
    }
    @Query("""
        select c.id as id, c.fullName as fullName, c.phone as phone,
               c.candidateType as candidateType, c.location as location, c.availability as availability
        from CandidateProfile c where c.user.accountStatus = com.marketplace.user.AccountStatus.ACTIVE
          and c.user.role = com.marketplace.user.Role.CANDIDATE
          and (:name = '' or locate(lower(:name), lower(c.fullName)) > 0)
          and (:phone = '' or locate(:phone, c.phone) > 0)
          and (:track is null or c.candidateType = :track)
          and (:skillId is null or exists (select cs.id from CandidateSkill cs where cs.candidate = c and cs.skill.id = :skillId))
        order by c.fullName, c.id
        """)
    java.util.List<TrainingCandidate> searchForTraining(@Param("name") String name, @Param("phone") String phone,
        @Param("track") CandidateType track, @Param("skillId") Long skillId, org.springframework.data.domain.Pageable page);

    Optional<CandidateProfile> findByUserId(Long userId);

    @Query("select c.id from CandidateProfile c where c.candidateType = :type order by c.id")
    java.util.List<Long> findIdsByCandidateType(@Param("type") CandidateType type);

    // Lock the candidate before multi-skill reservation or placement changes.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CandidateProfile c where c.id = :id")
    Optional<CandidateProfile> findByIdForUpdate(@Param("id") Long id);
}

