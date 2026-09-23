package com.marketplace.job;

import com.marketplace.auth.JwtService;
import com.marketplace.candidate.*;
import com.marketplace.companytype.*;
import com.marketplace.employer.*;
import com.marketplace.skill.*;
import com.marketplace.user.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc @Transactional
class JobDiscoveryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired EmployerProfileRepository employers;
    @Autowired CandidateProfileRepository candidates;
    @Autowired JobRepository jobs;
    @Autowired CompanyTypeRepository types;
    @Autowired SkillRepository skills;
    @Autowired JobApplicationRepository applications;
    User candidate;
    EmployerProfile employer;
    CompanyType sector;
    Job junior, senior;
    String marker;

    User user(Role role) {
        var u=new User();u.setRole(role);u.setEmail(UUID.randomUUID()+"@example.test");u.setPasswordHash("test-only");
        return users.saveAndFlush(u);
    }
    Job job(String title,int months,JobStatus status) {
        var j=new Job();j.setEmployer(employer);j.setTitle(title);j.setDescription(marker+" Build reliable products");
        j.setLocation("Dhaka, Bangladesh");j.setPublicExpectations("Communicate clearly");j.setPrivateExpectations("PRIVATE_DISCOVERY_SECRET");
        j.setExpectedExperienceMonths(months);j.setCandidateType(CandidateType.TECH);j.setStatus(status);
        return jobs.saveAndFlush(j);
    }
    @BeforeEach void setup() {
        marker=UUID.randomUUID().toString();
        sector=new CompanyType();sector.setName("Discovery "+marker);sector.setNormalizedName(marker);types.saveAndFlush(sector);
        employer=new EmployerProfile();employer.setUser(user(Role.EMPLOYER));employer.setCompanyName("Discovery Company");employer.setCompanyType(sector);employers.saveAndFlush(employer);
        candidate=user(Role.CANDIDATE);var c=new CandidateProfile();c.setUser(candidate);c.setFullName("Job seeker");c.setCandidateType(CandidateType.TECH);candidates.saveAndFlush(c);
        junior=job("Junior Developer",0,JobStatus.ACTIVE);
        senior=job("Senior Engineer",24,JobStatus.ACTIVE);senior.setLocation("Chattogram");jobs.saveAndFlush(senior);
    }
    String auth(User u) { return "Bearer "+jwt.issue(u.getId()); }
    MockHttpServletRequestBuilder query() { return get("/api/candidates/me/jobs").header("Authorization",auth(candidate)).param("companyTypeId",sector.getId().toString()); }
    ResultActions search(String key,String value) throws Exception { return mvc.perform(query().param(key,value)).andExpect(status().isOk()); }
    ResultActions detail(Long id) throws Exception { return mvc.perform(get("/api/candidates/me/jobs/"+id).header("Authorization",auth(candidate))); }

    @Test void onlyActiveJobsFromActiveEmployersAreDiscoverable() throws Exception {
        var draft=job("Draft",0,JobStatus.DRAFT);var closed=job("Closed",0,JobStatus.CLOSED);
        mvc.perform(query()).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        detail(draft.getId()).andExpect(status().isNotFound());detail(closed.getId()).andExpect(status().isNotFound());
        employer.getUser().setAccountStatus(AccountStatus.SUSPENDED);users.flush();
        mvc.perform(query()).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        detail(junior.getId()).andExpect(status().isNotFound());
        mvc.perform(get("/api/candidates/me/jobs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/candidates/me/jobs").header("Authorization",auth(user(Role.EMPLOYER)))).andExpect(status().isForbidden());
    }
    @Test void publicTextSearchIncludesTitleDescriptionExpectationsCompanyAndSkill() throws Exception {
        search("search","jUnIoR").andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(junior.getId()));
        for(String term:List.of("RELIABLE","communicate", "discovery company")) search("search",term).andExpect(jsonPath("$.totalElements").value(2));
        var skill=new Skill();skill.setName("Java "+marker);skill.setCategory("TECH");skills.saveAndFlush(skill);junior.setRequiredSkill(skill);jobs.flush();
        search("search","JAVA").andExpect(jsonPath("$.totalElements").value(1));
        search("search","PRIVATE_DISCOVERY_SECRET").andExpect(jsonPath("$.totalElements").value(0));
        search("search","%").andExpect(jsonPath("$.totalElements").value(0));
        search("search","  ").andExpect(jsonPath("$.totalElements").value(2));
    }
    @Test void locationSectorExperienceAndCombinedFiltersUsePersistedFields() throws Exception {
        search("location","DHAKA").andExpect(jsonPath("$.content[0].id").value(junior.getId())).andExpect(jsonPath("$.totalElements").value(1));
        search("maxExperience","0").andExpect(jsonPath("$.totalElements").value(1));
        search("maxExperience","12").andExpect(jsonPath("$.totalElements").value(1));
        search("minExperience","24").andExpect(jsonPath("$.content[0].id").value(senior.getId()));
        mvc.perform(query().param("search","developer").param("location","dhaka").param("minExperience","0").param("maxExperience","12").param("candidateTrack","TECH"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        senior.setCandidateType(CandidateType.TRADE);jobs.flush();
        search("candidateTrack","TRADE").andExpect(jsonPath("$.content[0].id").value(senior.getId()));
        var other=types.findByCode("OTHER").orElseThrow();employer.setCompanyType(other);employer.setCustomCompanyType("Architecture Consultancy");employers.flush();
        mvc.perform(query()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/candidates/me/jobs").header("Authorization",auth(candidate)).param("companyTypeId",other.getId().toString()).param("search",marker))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.content[0].companyTypeName").value("Architecture Consultancy"));
        detail(junior.getId()).andExpect(jsonPath("$.companyTypeName").value("Architecture Consultancy")).andExpect(jsonPath("$.companyTypeId").value(other.getId()));
    }
    @Test void paginationAndAllowedSortingAreStable() throws Exception {
        mvc.perform(query().param("size","1")).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1)).andExpect(jsonPath("$.content[0].id").value(senior.getId()));
        mvc.perform(query().param("size","1").param("page","1")).andExpect(jsonPath("$.content[0].id").value(junior.getId()));
        mvc.perform(query().param("page","99")).andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.totalElements").value(2));
        search("sort","oldest").andExpect(jsonPath("$.content[0].id").value(junior.getId()));
        search("sort","experienceAsc").andExpect(jsonPath("$.content[0].id").value(junior.getId()));
        search("sort","experienceDesc").andExpect(jsonPath("$.content[0].id").value(senior.getId()));
    }
    @Test void candidateDtoAndApplicationStateStayPrivateAndConsistent() throws Exception {
        search("search","junior").andExpect(jsonPath("$.content[0].hasApplied").value(false)).andExpect(jsonPath("$.content[0].applicationStatus").isEmpty());
        detail(junior.getId()).andExpect(status().isOk()).andExpect(jsonPath("$.publicExpectations").value("Communicate clearly"))
            .andExpect(jsonPath("$.expectedExperienceMonths").value(0)).andExpect(jsonPath("$.hasApplied").value(false));
        mvc.perform(post("/api/jobs/"+junior.getId()+"/apply").header("Authorization",auth(candidate))).andExpect(status().isCreated()).andExpect(jsonPath("$.job.hasApplied").value(true));
        var a=applications.findByJobIdAndCandidateId(junior.getId(),candidates.findByUserId(candidate.getId()).orElseThrow().getId()).orElseThrow();
        search("search","junior").andExpect(jsonPath("$.content[0].applicationStatus").value("APPLIED"));
        a.setStatus(ApplicationStatus.UNDER_REVIEW);applications.flush();
        String list=search("search","junior").andExpect(jsonPath("$.content[0].hasApplied").value(true)).andExpect(jsonPath("$.content[0].applicationStatus").value("UNDER_REVIEW")).andReturn().getResponse().getContentAsString();
        String detail=detail(junior.getId()).andExpect(jsonPath("$.hasApplied").value(true)).andExpect(jsonPath("$.applicationStatus").value("UNDER_REVIEW")).andReturn().getResponse().getContentAsString();
        assertThat(list+detail).doesNotContain("privateExpectations","PRIVATE_DISCOVERY_SECRET","passwordHash","contactPhone","applicants");
        mvc.perform(post("/api/jobs/"+junior.getId()+"/apply").header("Authorization",auth(candidate))).andExpect(status().isConflict());
        junior.setStatus(JobStatus.CLOSED);jobs.flush();
        // Existing applicants retain historical detail access, but closed jobs leave discovery.
        detail(junior.getId()).andExpect(status().isOk());search("search","junior").andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void malformedAndUnsupportedFiltersAreRejected() throws Exception {
        for(var input:List.of(new String[]{"page","-1"},new String[]{"page","2147483647"},new String[]{"size","0"},new String[]{"size","51"},
            new String[]{"minExperience","-1"},new String[]{"maxExperience","-1"},new String[]{"page","abc"},
            new String[]{"minExperience","1.5"},new String[]{"candidateTrack","INVALID"},new String[]{"sort","privateExpectations"},
            new String[]{"search","x".repeat(201)},new String[]{"location","x".repeat(256)}))
            mvc.perform(query().param(input[0],input[1])).andExpect(status().isBadRequest());
        for(String id:List.of("-1","999999999","abc")) mvc.perform(get("/api/candidates/me/jobs").header("Authorization",auth(candidate)).param("companyTypeId",id)).andExpect(status().isBadRequest());
        mvc.perform(query().param("minExperience","25").param("maxExperience","12")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/candidates/me/jobs").header("Authorization",auth(candidate)).param("companyTypeId","").param("location","").param("search","").param("minExperience","")).andExpect(status().isOk());
    }
}
