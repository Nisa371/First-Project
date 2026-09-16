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
 private final ApplicationEventPublisher events;
 public record Account(Long id,String email,Role role,AccountStatus status) {}
 public record Stats(long users,long activeUsers,long jobs,long pendingVerifications,long activePlacements,long replacements) {}
 public record JobView(Long id,String title,String company,String location,JobStatus status) {}
 public record Verification(Long id,String candidate,VerificationStatus status,java.time.Instant submittedAt) {}
 public record SkillView(Long id,String name,String category,boolean active) {}
 private User admin(){var u=account.requireActive();if(u.getRole()!=Role.ADMIN)throw new ApiException(403,"FORBIDDEN","Admin access required.");return u;}
 private Account view(User u){return new Account(u.getId(),u.getEmail(),u.getRole(),u.getAccountStatus());}
 public Stats stats(){admin();return new Stats(users.count(),users.findAll().stream().filter(u->u.getAccountStatus()==AccountStatus.ACTIVE).count(),jobs.count(),verifications.findByStatusOrderBySubmittedAtAsc(VerificationStatus.PENDING).size(),placements.findAll().stream().filter(p->p.getStatus()==PlacementStatus.ACTIVE).count(),replacements.count());}
 public List<Account> users(){admin();return users.findAll(org.springframework.data.domain.Sort.by("id")).stream().map(this::view).toList();}
 public List<JobView> jobs(){admin();return jobs.findAll().stream().map(j->new JobView(j.getId(),j.getTitle(),j.getEmployer().getCompanyName(),j.getLocation(),j.getStatus())).toList();}
 public List<Verification> verifications(){admin();return verifications.findAll(org.springframework.data.domain.Sort.by("submittedAt").descending()).stream().map(v->new Verification(v.getId(),v.getCandidate().getFullName(),v.getStatus(),v.getSubmittedAt())).toList();}
 public List<SkillView> skills(){admin();return skills.findAll().stream().map(s->new SkillView(s.getId(),s.getName(),s.getCategory(),s.isActive())).toList();}
 @Transactional public Account status(Long id,AccountStatus status){var actor=admin();var u=users.findByIdForUpdate(id).orElseThrow(()->new ApiException(404,"NOT_FOUND","Account not found."));
  if(u.getRole()==Role.ADMIN)throw new ApiException(409,"ADMIN_PROTECTED","Administrator accounts are protected from status changes in this console.");
  if(u.getAccountStatus()!=status){u.setAccountStatus(status);events.publishEvent(new MarketplaceEvent(actor,"ACCOUNT_STATUS_"+status,"USER",u.getId(),List.of(u),"Account status updated","Your account status is now "+status+"."));}return view(u);
 }
}
