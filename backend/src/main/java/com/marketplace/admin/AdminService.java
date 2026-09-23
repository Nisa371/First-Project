package com.marketplace.admin;
import com.marketplace.auth.CurrentAccount;
import com.marketplace.user.*;
import com.marketplace.job.*;
import com.marketplace.skill.*;
import com.marketplace.verification.*;
import com.marketplace.placement.*;
import com.marketplace.replacement.*;
import com.marketplace.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AdminService {
 private final CurrentAccount account; private final UserRepository users; private final JobRepository jobs;
 private final SkillRepository skills; private final VerificationRecordRepository verifications;
 private final PlacementRepository placements; private final ReplacementRequestRepository replacements;
 private final jakarta.persistence.EntityManager em;
 private final ApplicationEventPublisher events;
 public record Account(Long id,String email,Role role,AccountStatus status) {}
 public record Stats(long users,long activeUsers,long jobs,long pendingVerifications,long activePlacements,long replacements,long candidates,long employers,long activeJobs,long applications,long pendingBookings,long demoTransactions) {}
 public record JobView(Long id,String title,String company,String location,JobStatus status) {}
 public record Verification(Long id,String candidate,VerificationStatus status,java.time.Instant submittedAt) {}
 public record SkillView(Long id,String name,String category,boolean active) {}
 private User admin(){var u=account.requireActive();if(u.getRole()!=Role.ADMIN)throw new ApiException(403,"FORBIDDEN","Admin access required.");return u;}
 private Account view(User u){return new Account(u.getId(),u.getEmail(),u.getRole(),u.getAccountStatus());}
 private long count(String query) { return em.createQuery(query,Long.class).getSingleResult(); }
 public Stats stats(){admin();return new Stats(users.count(),count("select count(u) from User u where u.accountStatus = com.marketplace.user.AccountStatus.ACTIVE"),jobs.count(),count("select count(v) from VerificationRecord v where v.status in (com.marketplace.verification.VerificationStatus.PENDING, com.marketplace.verification.VerificationStatus.IN_REVIEW)"),count("select count(p) from Placement p where p.status = com.marketplace.placement.PlacementStatus.ACTIVE"),replacements.count(),count("select count(c) from CandidateProfile c"),count("select count(e) from EmployerProfile e"),count("select count(j) from Job j where j.status = com.marketplace.job.JobStatus.ACTIVE"),count("select count(a) from JobApplication a"),count("select count(b) from Booking b where b.status = com.marketplace.booking.BookingStatus.PENDING_PAYMENT"),count("select count(p) from PaymentTransaction p"));}
 public List<Account> users(){admin();return users.findAll(org.springframework.data.domain.PageRequest.of(0,50,org.springframework.data.domain.Sort.by("id"))).stream().map(this::view).toList();}
 public List<JobView> jobs(){admin();return jobs.findAll(org.springframework.data.domain.PageRequest.of(0,50)).stream().map(j->new JobView(j.getId(),j.getTitle(),j.getEmployer().getCompanyName(),j.getLocation(),j.getStatus())).toList();}
 public List<Verification> verifications(){admin();return verifications.findAll(org.springframework.data.domain.PageRequest.of(0,50,org.springframework.data.domain.Sort.by("submittedAt").descending())).stream().map(v->new Verification(v.getId(),v.getCandidate()==null?"Employer verification":v.getCandidate().getFullName(),v.getStatus(),v.getSubmittedAt())).toList();}
 public List<SkillView> skills(){admin();return skills.findAll(org.springframework.data.domain.PageRequest.of(0,50)).stream().map(s->new SkillView(s.getId(),s.getName(),s.getCategory(),s.isActive())).toList();}
 @Transactional public Account status(Long id,AccountStatus status){var actor=admin();var u=users.findByIdForUpdate(id).orElseThrow(()->new ApiException(404,"NOT_FOUND","Account not found."));
  if(u.getRole()==Role.ADMIN)throw new ApiException(409,"ADMIN_PROTECTED","Administrator accounts are protected from status changes in this console.");
  if(u.getAccountStatus()!=status){u.setAccountStatus(status);events.publishEvent(new MarketplaceEvent(actor,"ACCOUNT_STATUS_"+status,"USER",u.getId(),List.of(u),"Account status updated","Your account status is now "+status+"."));}return view(u);
 }
}
