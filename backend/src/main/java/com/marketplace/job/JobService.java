package com.marketplace.job;
import com.marketplace.auth.CurrentAccount;
import com.marketplace.employer.EmployerService;
import com.marketplace.candidate.*;
import com.marketplace.skill.SkillRepository;
import com.marketplace.common.api.ApiException;
import com.marketplace.user.AccountStatus;
import com.marketplace.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import static com.marketplace.job.JobDtos.*;

@Service
@RequiredArgsConstructor
@Transactional
@PreAuthorize("hasRole('EMPLOYER')")
public class JobService {
    private final JobRepository jobs;
    private final EmployerService employers;
    private final CurrentAccount current;
    private final SkillRepository skills;
    private final ShortlistEntryRepository shortlists;
    private final CandidateProfileRepository candidates;
    private final CandidateSkillRepository candidateSkills;
    private final CandidateService candidateViews;
    public List<JobView> list() { return jobs.findByEmployerIdOrderByCreatedAtDesc(employers.own().getId()).stream().map(this::view).toList(); }
    public JobView get(Long id) { return view(owned(id)); }
    public JobView create(JobRequest r) {
        var j=new Job(); j.setEmployer(employers.own()); j.setStatus(JobStatus.ACTIVE); apply(j,r);
        return view(jobs.saveAndFlush(j));
    }
    public JobView update(Long id, JobRequest r) {
        var j=owned(id); requireOpen(j); apply(j,r); return view(j);
    }
    public JobView close(Long id) { var j=owned(id); j.setStatus(JobStatus.CLOSED); return view(j); }
    private Job owned(Long id) {
        return jobs.findOwnedForUpdate(id,current.requireActive().getId()).orElseThrow(CandidateService::missing);
    }
    private void requireOpen(Job j) { if(j.getStatus()!=JobStatus.ACTIVE) throw new ApiException(409,"JOB_CLOSED","This job is closed. Create a new job to continue hiring."); }
    private void apply(Job j, JobRequest r) {
        j.setTitle(r.title().trim()); j.setDescription(r.description().trim()); j.setLocation(r.location().trim()); j.setCandidateType(r.candidateType());
        j.setRequiredSkill(r.requiredSkillId()==null ? null : skills.findById(r.requiredSkillId()).filter(s->s.isActive()).orElseThrow(CandidateService::missing));
    }
    private JobView view(Job j) {
        return new JobView(j.getId(),j.getTitle(),j.getDescription(),j.getLocation(),j.getCandidateType(),
            j.getRequiredSkill()==null?null:j.getRequiredSkill().getId(),j.getRequiredSkill()==null?null:j.getRequiredSkill().getName(),
            j.getStatus(),shortlists.countByJobId(j.getId()),j.getCreatedAt());
    }
    public List<CandidateDtos.CandidateCard> shortlist(Long id) {
        owned(id); return shortlists.findByJobIdAndJobEmployerUserId(id,current.requireActive().getId()).stream()
            .filter(s->s.getCandidate().getUser().getAccountStatus()==AccountStatus.ACTIVE && s.getCandidate().getUser().getRole()==Role.CANDIDATE)
            .map(s->candidateViews.card(s.getCandidate())).toList();
    }
    public void add(Long id, Long candidateId) {
        var j=owned(id); requireOpen(j);
        var c=candidates.findById(candidateId).orElseThrow(CandidateService::missing);
        if(c.getUser().getAccountStatus()!=AccountStatus.ACTIVE || c.getUser().getRole()!=Role.CANDIDATE) throw CandidateService.missing();
        if(c.getAvailability()!=Availability.AVAILABLE || c.getCandidateType()!=j.getCandidateType()
            || (j.getRequiredSkill()!=null && !candidateSkills.existsByCandidateIdAndSkillId(c.getId(),j.getRequiredSkill().getId())))
            throw new ApiException(409,"CANDIDATE_MISMATCH","Candidate must be available and match the job track and required skill.");
        if(shortlists.existsByJobIdAndCandidateId(id,candidateId)) throw new ApiException(409,"DUPLICATE_SHORTLIST","This candidate is already shortlisted.");
        var entry=new ShortlistEntry(); entry.setJob(j); entry.setCandidate(c); shortlists.saveAndFlush(entry);
    }
    public void remove(Long id, Long candidateId) {
        owned(id); shortlists.findByJobIdAndCandidateId(id,candidateId).ifPresent(shortlists::delete);
    }
}
