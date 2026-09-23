package com.marketplace.interview;
import com.marketplace.auth.JwtService;
import com.marketplace.candidate.*;
import com.marketplace.employer.*;
import com.marketplace.job.*;
import com.marketplace.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
abstract class AssessmentTestSupport {
    @Autowired MockMvc mvc; @Autowired JwtService jwt; @Autowired ObjectMapper mapper;
    @Autowired UserRepository users; @Autowired EmployerProfileRepository employers;
    @Autowired CandidateProfileRepository candidates; @Autowired JobRepository jobs;
    @Autowired JobApplicationRepository applications;
    @Autowired AssessmentSessionRepository sessions; @Autowired AssessmentMessageRepository messages;
    @Autowired ApplicationEvaluationService scores;
    User user(Role role) {
        var u=new User(); u.setRole(role); u.setEmail(UUID.randomUUID()+"@example.test"); u.setPasswordHash("test-only");
        return users.saveAndFlush(u);
    }
    User employer() {
        var u=user(Role.EMPLOYER); var e=new EmployerProfile(); e.setUser(u); e.setCompanyName("Interview company");
        employers.saveAndFlush(e); return u;
    }
    CandidateProfile candidate(int months) {
        var c=new CandidateProfile(); c.setUser(user(Role.CANDIDATE)); c.setFullName("Applicant");
        c.setCandidateType(CandidateType.TECH); c.setAvailability(Availability.AVAILABLE); c.setTotalExperienceMonths(months);
        return candidates.saveAndFlush(c);
    }
    Job job(User owner) {
        var j=new Job(); j.setEmployer(employers.findByUserId(owner.getId()).orElseThrow()); j.setTitle("API developer");
        j.setDescription("Design REST APIs and database indexes"); j.setPublicExpectations("Explain API design");
        j.setPrivateExpectations("PRIVATE_INTERNAL_CRITERION"); j.setExpectedExperienceMonths(12);
        j.setLocation("Dhaka"); j.setCandidateType(CandidateType.TECH); j.setStatus(JobStatus.ACTIVE);
        return jobs.saveAndFlush(j);
    }
    JobApplication apply(Job j,CandidateProfile c) {
        var a=new JobApplication(); a.setJob(j); a.setCandidate(c); return applications.saveAndFlush(a);
    }
    String auth(User u) { return "Bearer "+jwt.issue(u.getId()); }
    String candidatePath(JobApplication a) { return "/api/candidate/applications/"+a.getId()+"/assessment"; }
    String employerPath(JobApplication a) { return "/api/employer/applications/"+a.getId()+"/assessment"; }
    ResultActions start(User u, JobApplication a) throws Exception { return mvc.perform(post(candidatePath(a)+"/start").header("Authorization",auth(u))); }
    ResultActions getSession(User u,JobApplication a) throws Exception { return mvc.perform(get(candidatePath(a)).header("Authorization",auth(u))); }
    ResultActions answer(User u,long id,int turn,String response) throws Exception {
        return mvc.perform(post("/api/candidate/assessment/"+id+"/messages").param("expectedTurn",String.valueOf(turn))
            .header("Authorization",auth(u)).contentType("application/json").content(mapper.writeValueAsString(java.util.Map.of("response",response))));
    }
    ResultActions review(User u,JobApplication a) throws Exception { return mvc.perform(get(employerPath(a)).header("Authorization",auth(u))); }
    ResultActions retry(User u,JobApplication a) throws Exception { return mvc.perform(post(employerPath(a)+"/retry-evaluation").header("Authorization",auth(u))); }
    long sessionId(JobApplication a) { return sessions.findByJobApplicationId(a.getId()).orElseThrow().getId(); }
}
