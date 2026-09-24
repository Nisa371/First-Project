package com.marketplace.interview;

import com.marketplace.ai.*;
import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.CandidateService;
import com.marketplace.common.api.ApiException;
import com.marketplace.job.*;
import com.marketplace.user.AccountStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static com.marketplace.interview.AssessmentDtos.*;
import static com.marketplace.interview.AssessmentSession.Status.*;
import static com.marketplace.ai.AssessmentProviderDtos.*;

// READ_COMMITTED ensures queries after waiting for a lock see the latest transcript on MySQL too.
@Service @lombok.RequiredArgsConstructor
@Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class ApplicationAssessmentService {
    private final JobApplicationRepository applications;
    private final JobRepository jobs;
    private final AssessmentSessionRepository sessions;
    private final AssessmentMessageRepository messages;
    private final CurrentAccount current;
    private final AiAssessmentProvider provider;
    private final AssessmentContextBuilder context;
    private final AssessmentConfiguration config;
    private final ApplicationEvaluationService scores;

    @PreAuthorize("hasRole('CANDIDATE')")
    public SessionView get(Long applicationId) {
        var a=owned(applicationId, false);
        return view(a, sessions.findByJobApplicationId(applicationId).orElse(null));
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public SessionView start(Long applicationId) {
        var a=owned(applicationId, false);
        var s=sessions.findByJobApplicationId(applicationId).orElse(null);
        if (s!=null && s.getStatus()!=NOT_STARTED) return view(a,s);
        requireAllowed(a);
        if (s==null) { s=new AssessmentSession(); s.setJobApplication(a); s.setMaxTurns(config.getMaxTurns()); sessions.save(s); }
        String opening;
        try {
            var result=provider.generateNextTurn(context.turn(a,List.of(),0,s.getMaxTurns()));
            if (result==null || result.complete()) throw new AssessmentContextBuilder.InvalidOutput();
            opening=context.visible(result.message(),a);
        } catch (RuntimeException e) { s.setFailureCode(failure(e)); return view(a,s); }
        s.setFailureCode(null); s.setStatus(IN_PROGRESS); s.setStartedAt(Instant.now());
        messages.save(new AssessmentMessage(s,AssessmentMessage.SenderRole.AI,opening,1));
        return view(a,s);
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public SessionView answer(Long sessionId, int expectedTurn, Answer input) {
        var appId=sessions.applicationId(sessionId).orElseThrow(CandidateService::missing);
        var a=owned(appId,false);
        var s=sessions.findByJobApplicationId(appId).orElseThrow(CandidateService::missing);
        requireAllowed(a);
        if (s.getStatus()!=IN_PROGRESS) throw conflict("ASSESSMENT_CLOSED","This assessment is not accepting answers.");
        if (s.getCurrentTurn()!=expectedTurn) throw conflict("STALE_ASSESSMENT_TURN","This turn was already answered. Reload the assessment.");
        if (input.response()==null || input.response().isBlank() || input.response().length()>MAX_ANSWER_LENGTH)
            throw new ApiException(400,"INVALID_ANSWER","Enter an answer of up to "+MAX_ANSWER_LENGTH+" characters.");
        var transcript=new ArrayList<>(transcript(s));
        String answer=input.response().strip();
        int sequence=transcript.size()+1, nextTurn=s.getCurrentTurn()+1;
        transcript.add(new TranscriptEntry("CANDIDATE",answer,sequence));
        boolean complete=nextTurn>=s.getMaxTurns();
        String reply=null;
        if (!complete) {
            try {
                var result=provider.generateNextTurn(context.turn(a,transcript,nextTurn,s.getMaxTurns()));
                if (result==null) throw new AssessmentContextBuilder.InvalidOutput();
                reply=context.visible(result.message(),a); complete=result.complete();
            } catch (RuntimeException e) { s.setFailureCode(failure(e)); return view(a,s); }
        }
        // Commit candidate and AI messages together only after a usable turn response.
        messages.save(new AssessmentMessage(s,AssessmentMessage.SenderRole.CANDIDATE,answer,sequence));
        if (reply!=null) {
            messages.save(new AssessmentMessage(s,AssessmentMessage.SenderRole.AI,reply,sequence+1));
            transcript.add(new TranscriptEntry("AI",reply,sequence+1));
        }
        s.setCurrentTurn(nextTurn); s.setFailureCode(null);
        if (complete) { s.setCompletedAt(Instant.now()); evaluate(s,transcript); }
        return view(a,s);
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public EmployerView review(Long applicationId) {
        var a=owned(applicationId,true); var s=sessions.findByJobApplicationId(applicationId).orElse(null);
        return new EmployerView(view(a,s),a.getAssessmentScore(),s==null?null:s.getEvaluationSummary());
    }
    // Explicit retry only for failed final evaluation; the candidate transcript remains closed.
    @PreAuthorize("hasRole('EMPLOYER')")
    public EmployerView retry(Long applicationId) {
        var a=owned(applicationId,true); var s=sessions.findByJobApplicationId(applicationId).orElseThrow(CandidateService::missing);
        if (s.getStatus()!=FAILED || s.getCompletedAt()==null) throw conflict("INVALID_STATE","Only a failed final evaluation can be retried.");
        if ("INVALID_AI_RESPONSE".equals(s.getFailureCode())) throw conflict("INVALID_STATE","The provider returned invalid output. Internal review is required before retrying.");
        evaluate(s,transcript(s));
        return new EmployerView(view(a,s),a.getAssessmentScore(),s.getEvaluationSummary());
    }
    private void evaluate(AssessmentSession s, List<TranscriptEntry> transcript) {
        BigDecimal score; String summary;
        try {
            var result=provider.evaluateAssessment(context.evaluation(s.getJobApplication(),transcript));
            Double raw=result==null?null:result.score();
            if (raw==null || !Double.isFinite(raw) || raw<0 || raw>1) throw new AssessmentContextBuilder.InvalidOutput();
            // Match Phase 7 storage precision after validating the raw value; never clamp invalid output.
            score=BigDecimal.valueOf(raw).setScale(16,java.math.RoundingMode.HALF_UP);
            summary=result.summary();
            if (summary!=null && summary.length()>2000) throw new AssessmentContextBuilder.InvalidOutput();
        } catch (RuntimeException e) { s.setStatus(FAILED); s.setFailureCode(failure(e)); return; }
        // Database errors propagate and roll back; provider failures never overwrite valid scores.
        scores.updateAssessmentScore(s.getJobApplication().getId(),score);
        s.setEvaluationSummary(summary); s.setStatus(COMPLETED); s.setFailureCode(null);
    }
    private JobApplication owned(Long applicationId, boolean employer) {
        Long userId=current.requireActive().getId();
        Long jobId=(employer?applications.employerJobId(applicationId,userId):applications.ownedJobId(applicationId,userId))
            .orElseThrow(CandidateService::missing);
        // Same job -> application lock order as existing evaluation/ranking/withdrawal workflows.
        jobs.findByIdForUpdate(jobId).orElseThrow(CandidateService::missing);
        var a=applications.findForEvaluation(applicationId).orElseThrow(CandidateService::missing);
        return a;
    }
    private boolean allowed(JobApplication a) {
        return a.getStatus()!=ApplicationStatus.WITHDRAWN && a.getStatus()!=ApplicationStatus.REJECTED
            && a.getJob().getStatus()==JobStatus.ACTIVE && a.getJob().getEmployer().getUser().getAccountStatus()==AccountStatus.ACTIVE;
    }
    private void requireAllowed(JobApplication a) {
        if (!allowed(a)) throw conflict("ASSESSMENT_UNAVAILABLE","Assessments are unavailable for closed jobs or withdrawn/rejected applications.");
    }
    private List<TranscriptEntry> transcript(AssessmentSession s) {
        return messages.findByAssessmentSessionIdOrderBySequenceNumberAsc(s.getId()).stream()
            .map(m -> new TranscriptEntry(m.getSenderRole().name(),m.getContent(),m.getSequenceNumber())).toList();
    }
    private SessionView view(JobApplication a, AssessmentSession s) {
        var transcript=s==null?List.<Message>of():messages.findByAssessmentSessionIdOrderBySequenceNumberAsc(s.getId()).stream()
            .map(m -> new Message(m.getSenderRole().name(),m.getContent(),m.getSequenceNumber(),m.getCreatedAt())).toList();
        return new SessionView(s==null?null:s.getId(),a.getId(),a.getJob().getTitle(),s==null?"NOT_STARTED":s.getStatus().name(),
            s==null?0:s.getCurrentTurn(),s==null?config.getMaxTurns():s.getMaxTurns(),s==null?null:s.getStartedAt(),
            s==null?null:s.getCompletedAt(),s==null?null:s.getFailureCode(),
            allowed(a) && (s==null || s.getStatus()==NOT_STARTED), allowed(a) && s!=null && s.getStatus()==IN_PROGRESS,transcript);
    }
    private String failure(RuntimeException e) {
        if (e instanceof AiEvaluationUnavailableException) return "AI_PROVIDER_NOT_CONFIGURED";
        if (e instanceof AssessmentContextBuilder.InvalidOutput || e instanceof AiProviderInvalidResponseException) return "INVALID_AI_RESPONSE";
        return "AI_ASSESSMENT_FAILED";
    }
    private static ApiException conflict(String code,String message) { return new ApiException(409,code,message); }
}
