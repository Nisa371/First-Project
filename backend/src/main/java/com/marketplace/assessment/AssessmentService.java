package com.marketplace.assessment;
import java.util.*;
import java.time.Instant;
import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.*;
import com.marketplace.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;
import static com.marketplace.assessment.AssessmentDtos.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentService {
    private final CurrentAccount current;
    private final CandidateProfileRepository candidates;
    private final AssessmentRepository assessments;
    private final AssessmentQuestionRepository questions;
    private final AssessmentAttemptRepository attempts;
    private final EvaluationRepository evaluations;
    private final AssessmentStrategyFactory strategies;
    private final ObjectMapper mapper;
    private CandidateProfile candidate() {
        return candidates.findByUserId(current.requireActive().getId()).orElseThrow(AssessmentService::missing);
    }
    private Assessment eligible(Long id) {
        var a=assessments.findById(id).orElseThrow(AssessmentService::missing);
        if (!a.isActive() || a.getCandidateType()!=candidate().getCandidateType()) throw missing();
        return a;
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<AssessmentView> list() {
        return assessments.findByCandidateTypeAndActiveTrue(candidate().getCandidateType()).stream()
            .filter(a -> !questions.findByAssessmentIdOrderByIdAsc(a.getId()).isEmpty()).map(this::assessmentView).toList();
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public AssessmentView get(Long id) { return assessmentView(eligible(id)); }
    @PreAuthorize("hasRole('CANDIDATE')")
    public AttemptView start(Long id) {
        var c=candidate(); candidates.findByIdForUpdate(c.getId()).orElseThrow(AssessmentService::missing);
        var a=eligible(id);
        if(questions.findByAssessmentIdOrderByIdAsc(id).isEmpty()) throw conflict("This assessment has no questions yet.");
        // One attempt per assessment in this MVP; retries resume the existing work.
        var existing=attempts.findByCandidateIdOrderByStartedAtDesc(c.getId()).stream()
            .filter(t -> t.getAssessment().getId().equals(id)).findFirst();
        if(existing.isPresent()) return view(existing.get());
        var t=new AssessmentAttempt(); t.setCandidate(c); t.setAssessment(a);
        return view(attempts.saveAndFlush(t));
    }
    private AssessmentAttempt own(Long id) {
        var t=attempts.findByIdForUpdate(id).orElseThrow(AssessmentService::missing);
        if(!t.getCandidate().getUser().getId().equals(current.requireActive().getId())) throw missing();
        return t;
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<AttemptView> mine() { return attempts.findByCandidateIdOrderByStartedAtDesc(candidate().getId()).stream().map(this::view).toList(); }
    @PreAuthorize("hasRole('CANDIDATE')")
    public AttemptView detail(Long id) { return view(own(id)); }
    private void inProgress(AssessmentAttempt t) {
        if(t.getStatus()!=AttemptStatus.IN_PROGRESS) throw conflict("Submitted answers cannot be changed.");
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public AttemptView save(Long id, AnswersRequest r) {
        var t=own(id); inProgress(t);
        var ids=questions.findByAssessmentIdOrderByIdAsc(t.getAssessment().getId()).stream().map(AssessmentQuestion::getId).toList();
        if(!ids.containsAll(r.answers().keySet())) throw new ApiException(400,"INVALID_ANSWER","An answer does not belong to this assessment.");
        t.setAnswersJson(mapper.writeValueAsString(r.answers())); return view(t);
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public AttemptView submit(Long id) {
        var t=own(id);
        if(t.getStatus()!=AttemptStatus.IN_PROGRESS) return view(t);
        var qs=questions.findByAssessmentIdOrderByIdAsc(t.getAssessment().getId());
        var answers=answers(t);
        if(qs.isEmpty() || !answers.keySet().containsAll(qs.stream().map(AssessmentQuestion::getId).toList()))
            throw new ApiException(400,"INCOMPLETE_ATTEMPT","Answer every question before submitting.");
        t.setAutoScore(strategies.forType(t.getAssessment().getCandidateType()).score(qs,answers));
        t.setStatus(AttemptStatus.SUBMITTED); t.setSubmittedAt(Instant.now()); return view(t);
    }
    @PreAuthorize("hasRole('EVALUATOR')")
    public List<ReviewView> queue() {
        current.requireActive();
        return attempts.findAll(org.springframework.data.domain.Sort.by("submittedAt").ascending()).stream()
            .filter(t -> t.getStatus()!=AttemptStatus.IN_PROGRESS).map(this::reviewView).toList();
    }
    @PreAuthorize("hasRole('EVALUATOR')")
    public ReviewView review(Long id) { return reviewView(reviewable(id)); }
    private AssessmentAttempt reviewable(Long id) {
        current.requireActive();
        var t=attempts.findByIdForUpdate(id).orElseThrow(AssessmentService::missing);
        if(t.getStatus()==AttemptStatus.IN_PROGRESS) throw missing(); return t;
    }
    private void requireEditor(Evaluation e) {
        if(e.getEvaluatorUser()!=null && !e.getEvaluatorUser().getId().equals(current.requireActive().getId()))
            throw conflict("Another evaluator owns this review.");
        if(e.isReleased()) throw conflict("Released results are final.");
    }
    @PreAuthorize("hasRole('EVALUATOR')")
    public ReviewView evaluate(Long id, EvaluationRequest r) {
        var t=reviewable(id);
        var e=evaluations.findByAttemptId(id).orElseGet(Evaluation::new); requireEditor(e);
        e.setAttempt(t); e.setEvaluatorUser(current.requireActive()); e.setScore(r.score());
        e.setRecommendation(r.recommendation()); e.setCandidateFeedback(r.feedback().trim()); e.setInternalNotes(r.internalNotes());
        evaluations.saveAndFlush(e); return reviewView(t);
    }
    @PreAuthorize("hasRole('EVALUATOR')")
    public ReviewView release(Long id) {
        var t=reviewable(id); var e=evaluations.findByAttemptId(id).orElseThrow(() -> conflict("Save your review before releasing it."));
        requireEditor(e); e.setReleased(true); t.setStatus(AttemptStatus.EVALUATED); t.setEvaluatedAt(Instant.now());
        return reviewView(t);
    }
    private Map<Long,AnswerOption> answers(AssessmentAttempt t) {
        return mapper.readValue(t.getAnswersJson(),new TypeReference<Map<Long,AnswerOption>>() {});
    }
    private AssessmentView assessmentView(Assessment a) {
        return new AssessmentView(a.getId(),a.getTitle(),a.getCandidateType(),a.getPassingScore(),
            questions.findByAssessmentIdOrderByIdAsc(a.getId()).stream().map(q -> new QuestionView(q.getId(),q.getPrompt(),
                List.of(q.getOptionA(),q.getOptionB(),q.getOptionC(),q.getOptionD()))).toList());
    }
    private ResultView result(Evaluation e) { return new ResultView(e.getScore(),e.getRecommendation(),e.getCandidateFeedback()); }
    private AttemptView view(AssessmentAttempt t) {
        var released=evaluations.findByAttemptId(t.getId()).filter(Evaluation::isReleased);
        return new AttemptView(t.getId(),assessmentView(t.getAssessment()),t.getStatus(),answers(t),t.getStartedAt(),t.getSubmittedAt(),
            released.isPresent()?t.getAutoScore():null,released.map(this::result).orElse(null));
    }
    private ReviewView reviewView(AssessmentAttempt t) {
        var e=evaluations.findByAttemptId(t.getId());
        boolean editable=e.isEmpty() || (!e.get().isReleased() && (e.get().getEvaluatorUser()==null || e.get().getEvaluatorUser().getId().equals(current.requireActive().getId())));
        return new ReviewView(view(t),t.getCandidate().getFullName(),t.getAutoScore(),e.map(this::result).orElse(null),
            e.map(Evaluation::getInternalNotes).orElse(null),e.map(Evaluation::isReleased).orElse(false),editable);
    }
    private static ApiException missing() { return new ApiException(404,"NOT_FOUND","Assessment or attempt not found."); }
    private static ApiException conflict(String message) { return new ApiException(409,"INVALID_STATE",message); }
}
