package com.marketplace.candidate;

import com.marketplace.auth.JwtService;
import com.marketplace.employer.*;
import com.marketplace.job.*;
import com.marketplace.skill.*;
import com.marketplace.user.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class CandidateCvPhotoIntegrationTest {
    @TempDir static Path storage;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.photo.directory", () -> storage.resolve("photos").toString());
        r.add("app.cv.directory", () -> storage.resolve("pdfs").toString());
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired CandidateProfileRepository candidates;
    @Autowired EmployerProfileRepository employers;
    @Autowired JobRepository jobs;
    @Autowired JobApplicationRepository applications;
    @Autowired CandidateCvRepository cvs;
    @Autowired SkillRepository skills;
    @Autowired CandidateSkillRepository candidateSkills;

    User user(Role role) {
        var u = new User(); u.setRole(role); u.setEmail(UUID.randomUUID() + "@example.test");
        u.setPasswordHash("PRIVATE_HASH"); return users.saveAndFlush(u);
    }
    User candidate() {
        var u = user(Role.CANDIDATE); var c = new CandidateProfile(); c.setUser(u); c.setFullName("Candidate CV");
        c.setTotalExperienceMonths(18); c.setPortfolioUrl("https://example.com/portfolio");
        candidates.saveAndFlush(c); return u;
    }
    CandidateProfile profile(User u) { return candidates.findByUserId(u.getId()).orElseThrow(); }
    String auth(User u) { return "Bearer " + jwt.issue(u.getId()); }
    byte[] image(String format) throws Exception {
        var out = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), format, out); return out.toByteArray();
    }
    ResultActions upload(User u, byte[] bytes, String name, String type) throws Exception {
        return mvc.perform(multipart("/api/candidates/me/photo").file(new MockMultipartFile("file", name, type, bytes)).header("Authorization", auth(u)));
    }
    ResultActions save(User u, Map<String, Object> body) throws Exception {
        return mvc.perform(put("/api/candidates/me/built-cv").header("Authorization", auth(u)).contentType("application/json").content(mapper.writeValueAsString(body)));
    }
    Map<String, Object> cv() {
        var body = new LinkedHashMap<String, Object>(); body.put("summary", "Practical software engineer");
        body.put("linkedinUrl", "https://example.com/linkedin"); body.put("githubUrl", "https://example.com/github");
        body.put("education", List.of(Map.of("institution", "University", "qualification", "BSc", "fieldOfStudy", "Computing", "startDate", "2020-01-01", "endDate", "2024-01-01", "grade", "3.8"), Map.of("institution", "College", "qualification", "HSC")));
        body.put("experience", List.of(Map.of("organization", "Studio", "title", "Engineer", "startDate", "2024-01-01", "current", true, "description", "Built useful services")));
        body.put("projects", List.of(Map.of("name", "Marketplace", "technologies", "Java", "projectUrl", "https://example.com", "repositoryUrl", "https://example.com/repo")));
        body.put("certifications", List.of(Map.of("name", "Course", "organization", "Academy", "issueDate", "2024-02-01", "credentialUrl", "https://example.com/cert")));
        body.put("languages", List.of(Map.of("name", "Bangla", "proficiency", "Native")));
        body.put("achievements", List.of(Map.of("title", "Award", "issuer", "University", "awardDate", "2023-01-01", "description", "First prize")));
        return body;
    }
    JobApplication application(User employer, User candidate) {
        var e = new EmployerProfile(); e.setUser(employer); e.setCompanyName("Employer"); employers.saveAndFlush(e);
        var j = new Job(); j.setEmployer(e); j.setTitle("Role"); j.setDescription("Work"); j.setLocation("Dhaka"); j.setStatus(JobStatus.ACTIVE); jobs.saveAndFlush(j);
        var a = new JobApplication(); a.setJob(j); a.setCandidate(profile(candidate)); return applications.saveAndFlush(a);
    }
    @Test void validImagesAreReencodedStoredSafelyReplacedAndPubliclyRetrieved() throws Exception {
        var u = candidate();
        mvc.perform(get("/api/candidates/me").header("Authorization", auth(u))).andExpect(jsonPath("$.profilePhotoUrl").isEmpty());
        byte[] source = image("png");
        byte[] withTrailer = Arrays.copyOf(source, source.length + 11);
        System.arraycopy("PRIVATE_EXE".getBytes(), 0, withTrailer, source.length, 11);
        upload(u, withTrailer, "../../unsafe.exe", "application/octet-stream").andExpect(status().isOk())
            .andExpect(jsonPath("$.profilePhotoUrl").value("/api/candidates/" + profile(u).getId() + "/photo"))
            .andExpect(jsonPath("$.photoStoredName").doesNotExist());
        String first = profile(u).getPhotoStoredName();
        assertThat(first).matches("[a-f0-9-]{36}\\.png");
        byte[] stored = Files.readAllBytes(storage.resolve("photos").resolve(first));
        assertThat(new String(stored, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("PRIVATE_EXE");
        mvc.perform(get("/api/candidates/" + profile(u).getId() + "/photo")).andExpect(status().isOk())
            .andExpect(content().contentType("image/png")).andExpect(content().bytes(stored))
            .andExpect(header().string("X-Content-Type-Options", "nosniff")).andExpect(header().string("Cache-Control", "no-store"));
        upload(u, image("jpeg"), "replacement.jpg", "image/jpeg").andExpect(status().isOk());
        assertThat(profile(u).getPhotoStoredName()).isNotEqualTo(first).endsWith(".jpg");
        assertThat(Files.exists(storage.resolve("photos").resolve(first))).isFalse();
        mvc.perform(get("/api/candidates/" + profile(u).getId() + "/photo")).andExpect(content().contentType("image/jpeg"));
    }
    @Test void invalidImagesCannotChangeExistingPhoto() throws Exception {
        var u = candidate(); upload(u, image("png"), "a.png", "image/png").andExpect(status().isOk());
        String original = profile(u).getPhotoStoredName();
        for (byte[] bad : List.of(new byte[0], "<svg onload='alert(1)'/>".getBytes(), "not a png".getBytes(), new byte[CandidatePhotoService.MAX_BYTES + 1], Arrays.copyOf(image("png"), 20)))
            upload(u, bad, "fake.png", "image/png").andExpect(status().isBadRequest());
        upload(u, image("gif"), "fake.png", "image/png").andExpect(status().isBadRequest());
        var huge = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(4001, 4000, BufferedImage.TYPE_BYTE_GRAY), "png", huge);
        upload(u, huge.toByteArray(), "large.png", "image/png").andExpect(status().isBadRequest());
        assertThat(profile(u).getPhotoStoredName()).isEqualTo(original);
    }
    @Test void photoMutationIsAccountExclusiveAndMissingUnsafeOrInactiveImagesAreNotServed() throws Exception {
        var a = candidate(); var b = candidate();
        upload(a, image("png"), "a.png", "image/png").andExpect(status().isOk());
        String original = profile(a).getPhotoStoredName();
        upload(b, image("png"), "b.png", "image/png").andExpect(status().isOk());
        assertThat(profile(a).getPhotoStoredName()).isEqualTo(original);
        assertThat(profile(b).getPhotoStoredName()).isNotEqualTo(original);
        for (Role role : List.of(Role.EMPLOYER, Role.ADMIN, Role.EVALUATOR))
            upload(user(role), image("png"), "a.png", "image/png").andExpect(status().isForbidden());
        mvc.perform(multipart("/api/candidates/me/photo").file(new MockMultipartFile("file", "a.png", "image/png", image("png")))).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/candidates/" + profile(a).getId() + "/photo").file(new MockMultipartFile("file", "a.png", "image/png", image("png"))).header("Authorization", auth(b))).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/candidates/" + profile(candidate()).getId() + "/photo")).andExpect(status().isNotFound());
        var corrupted = profile(b); corrupted.setPhotoStoredName("../../application.yml"); candidates.saveAndFlush(corrupted);
        mvc.perform(get("/api/candidates/" + corrupted.getId() + "/photo")).andExpect(status().isNotFound());
        Files.delete(storage.resolve("photos").resolve(original));
        mvc.perform(get("/api/candidates/" + profile(a).getId() + "/photo")).andExpect(status().isNotFound());
        upload(a, image("png"), "a.png", "image/png").andExpect(status().isOk());
        a.setAccountStatus(AccountStatus.SUSPENDED); users.saveAndFlush(a);
        mvc.perform(get("/api/candidates/" + profile(a).getId() + "/photo")).andExpect(status().isNotFound());
    }
    @Test void cvRoundTripsAllSectionsUsesProfileSkillsAndPreservesUploadedPdfAndMonths() throws Exception {
        var u = candidate();
        mvc.perform(get("/api/candidates/me/built-cv").header("Authorization", auth(u))).andExpect(status().isOk()).andExpect(jsonPath("$.empty").value(true));
        mvc.perform(multipart("/api/candidates/me/cv").file(new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-1.4 preserved".getBytes())).header("Authorization", auth(u))).andExpect(status().isOk());
        String pdf = profile(u).getCvStoredName();
        mvc.perform(get("/api/candidates/me").header("Authorization", auth(u))).andExpect(jsonPath("$.hasBuiltCv").value(false));
        var skill = new Skill(); skill.setName("CV Skill " + UUID.randomUUID()); skill.setCategory("TECH"); skills.saveAndFlush(skill);
        var link = new CandidateSkill(); link.setSkill(skill); link.setCandidate(profile(u)); candidateSkills.saveAndFlush(link);
        save(u, cv()).andExpect(status().isOk()).andExpect(jsonPath("$.empty").value(false))
            .andExpect(jsonPath("$.content.education[0].institution").value("University"))
            .andExpect(jsonPath("$.content.experience[0].current").value(true))
            .andExpect(jsonPath("$.content.projects[0].name").value("Marketplace"))
            .andExpect(jsonPath("$.content.certifications[0].name").value("Course"))
            .andExpect(jsonPath("$.content.languages[0].name").value("Bangla"))
            .andExpect(jsonPath("$.content.achievements[0].title").value("Award"))
            .andExpect(jsonPath("$.header.skills[0].id").value(skill.getId()))
            .andExpect(jsonPath("$.header.portfolioUrl").value("https://example.com/portfolio"));
        mvc.perform(get("/api/candidates/me/built-cv").header("Authorization", auth(u))).andExpect(status().isOk()).andExpect(jsonPath("$.content.education.length()").value(2));
        mvc.perform(get("/api/candidates/me").header("Authorization", auth(u))).andExpect(jsonPath("$.hasBuiltCv").value(true));
        assertThat(profile(u).getCvStoredName()).isEqualTo(pdf); assertThat(profile(u).getTotalExperienceMonths()).isEqualTo(18);
        mvc.perform(get("/api/candidates/me/cv").header("Authorization", auth(u))).andExpect(content().bytes("%PDF-1.4 preserved".getBytes()));
    }
    @Test void updatingReorderingAndDeletingEntriesPersistsWithoutOrphans() throws Exception {
        var u = candidate(); save(u, cv()).andExpect(status().isOk());
        String firstUpdate = mapper.readTree(mvc.perform(get("/api/candidates/me/built-cv").header("Authorization", auth(u))).andReturn().getResponse().getContentAsString()).get("updatedAt").asText();
        var next = cv(); next.put("education", List.of(Map.of("institution", "College", "qualification", "HSC"), Map.of("institution", "Updated university", "qualification", "BSc")));
        next.put("projects", List.of()); save(u, next).andExpect(status().isOk());
        String response = mvc.perform(get("/api/candidates/me/built-cv").header("Authorization", auth(u)))
            .andExpect(jsonPath("$.content.education[0].institution").value("College"))
            .andExpect(jsonPath("$.content.education[1].institution").value("Updated university"))
            .andExpect(jsonPath("$.content.projects").isEmpty()).andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(response).get("updatedAt").asText()).isNotEqualTo(firstUpdate);
        for (String section : List.of("education", "experience", "projects", "certifications", "languages", "achievements")) next.put(section, List.of());
        next.put("summary", ""); save(u, next).andExpect(status().isOk()).andExpect(jsonPath("$.empty").value(true));
        mvc.perform(get("/api/candidates/me").header("Authorization", auth(u))).andExpect(jsonPath("$.hasBuiltCv").value(false));
    }
    @Test void malformedEntriesDatesUrlsAndClientControlledFieldsAreRejected() throws Exception {
        var u = candidate();
        var cases = List.of(
            Map.entry("education", List.of(Map.of("institution", " ", "qualification", "BSc"))),
            Map.entry("experience", List.of(Map.of("organization", "Studio", "title", ""))),
            Map.entry("education", List.of(Map.of("institution", "School", "qualification", "Degree", "startDate", "2024-01-01", "endDate", "2020-01-01"))),
            Map.entry("experience", List.of(Map.of("organization", "Studio", "title", "Engineer", "current", true, "endDate", "2025-01-01"))),
            Map.entry("projects", List.of(Map.of("name", "Project", "startDate", "2025-01-01", "endDate", "2020-01-01"))),
            Map.entry("projects", List.of(Map.of("name", "Project", "projectUrl", "javascript:alert(1)"))),
            Map.entry("certifications", List.of(Map.of("name", "Course", "organization", "Academy", "credentialUrl", "https://")))
        );
        for (var entry : cases) { var body = cv(); body.put(entry.getKey(), entry.getValue()); save(u, body).andExpect(status().isBadRequest()); }
        for (var invalid : Map.of("summary", "x".repeat(4001), "linkedinUrl", "https://user:pass@example.com", "githubUrl", "not a URL", "candidateId", "1").entrySet()) {
            var body = cv(); body.put(invalid.getKey(), invalid.getValue()); save(u, body).andExpect(status().isBadRequest());
        }
        var body = cv(); body.put("languages", Collections.singletonList(null)); save(u, body).andExpect(status().isBadRequest());
        body.put("languages", Collections.nCopies(31, Map.of("name", "Bangla", "proficiency", "Native"))); save(u, body).andExpect(status().isBadRequest());
        assertThat(cvs.findByCandidateId(profile(u).getId())).isEmpty();
    }
    @Test void candidateCvEditsAreExclusiveAndEmployerReadRequiresAnApplication() throws Exception {
        var a = candidate(); var b = candidate(); var employer = user(Role.EMPLOYER); var outsider = user(Role.EMPLOYER);
        save(a, cv()).andExpect(status().isOk());
        String publicPath = "/api/candidates/" + profile(a).getId() + "/built-cv";
        mvc.perform(get(publicPath).header("Authorization", auth(employer))).andExpect(status().isNotFound());
        var application = application(employer, a);
        var response = mvc.perform(get(publicPath).header("Authorization", auth(employer))).andExpect(status().isOk())
            .andExpect(jsonPath("$.header.fullName").value("Candidate CV")).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("PRIVATE_HASH", "password", "verification", "cvStoredName", "photoStoredName", "candidateId", "version", "NID", "reviewerNotes");
        mvc.perform(get(publicPath).header("Authorization", auth(outsider))).andExpect(status().isNotFound());
        mvc.perform(get(publicPath).header("Authorization", auth(b))).andExpect(status().isForbidden());
        mvc.perform(get(publicPath)).andExpect(status().isUnauthorized());
        mvc.perform(put(publicPath).header("Authorization", auth(b)).contentType("application/json").content(mapper.writeValueAsString(cv()))).andExpect(status().isMethodNotAllowed());
        for (Role role : List.of(Role.EMPLOYER, Role.ADMIN, Role.EVALUATOR)) {
            var u = user(role); save(u, cv()).andExpect(status().isForbidden());
            mvc.perform(get("/api/candidates/me/built-cv").header("Authorization", auth(u))).andExpect(status().isForbidden());
            if (role != Role.EMPLOYER) mvc.perform(get(publicPath).header("Authorization", auth(u))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/candidates/me/built-cv").header("Authorization", auth(b))).andExpect(jsonPath("$.empty").value(true));
        upload(a, image("png"), "a.png", "image/png").andExpect(status().isOk());
        mvc.perform(get("/api/jobs/" + application.getJob().getId() + "/applications").header("Authorization", auth(employer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].candidate.profilePhotoUrl").value("/api/candidates/" + profile(a).getId() + "/photo"));
        a.setAccountStatus(AccountStatus.SUSPENDED); users.saveAndFlush(a);
        mvc.perform(get(publicPath).header("Authorization", auth(employer))).andExpect(status().isNotFound());
    }
    @Test void profilePortfolioRejectsMalformedAndCredentialBearingUrls() throws Exception {
        var u=candidate();
        for(String url:List.of("https://user:secret@example.com","http://?query","https://#fragment")) {
            mvc.perform(put("/api/candidates/me").header("Authorization",auth(u)).contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("fullName","Candidate","availability","AVAILABLE","portfolioUrl",url))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("INVALID_CV_URL"));
        }
        assertThat(profile(u).getPortfolioUrl()).isEqualTo("https://example.com/portfolio");
    }
    @Test void pdfDownloadRejectsSymlinksAndTraversalAndReplacementCleansOldFile() throws Exception {
        var u=candidate();
        byte[] pdf="%PDF-1.4 resume".getBytes();
        mvc.perform(multipart("/api/candidates/me/cv").file(new MockMultipartFile("file","resume.pdf","application/pdf",pdf))
            .header("Authorization",auth(u))).andExpect(status().isOk());
        String first=profile(u).getCvStoredName();
        mvc.perform(multipart("/api/candidates/me/cv").file(new MockMultipartFile("file","next.pdf","application/pdf",pdf))
            .header("Authorization",auth(u))).andExpect(status().isOk());
        assertThat(Files.exists(storage.resolve("pdfs").resolve(first))).isFalse();
        var c=profile(u); var stored=storage.resolve("pdfs").resolve(c.getCvStoredName());
        Path outside=storage.resolve("private.txt"); Files.writeString(outside,"PRIVATE_DATA");
        Files.delete(stored); Files.createSymbolicLink(stored,outside);
        mvc.perform(get("/api/candidates/me/cv").header("Authorization",auth(u))).andExpect(status().isNotFound());
        c.setCvStoredName("../private.txt"); candidates.saveAndFlush(c);
        mvc.perform(get("/api/candidates/me/cv").header("Authorization",auth(u))).andExpect(status().isNotFound());
    }
    @Test void concurrentPdfReplacementsLeaveExactlyOneCurrentFile() throws Exception {
        var u=candidate(); String prefix=UUID.randomUUID().toString();
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var gate=new java.util.concurrent.CountDownLatch(1);
            var futures=new ArrayList<java.util.concurrent.Future<String>>();
            for(int i=0;i<2;i++) {
                final int index=i;
                futures.add(pool.submit(() -> {
                    gate.await();
                    mvc.perform(multipart("/api/candidates/me/cv").file(new MockMultipartFile("file","resume.pdf","application/pdf",
                        ("%PDF-1.4 "+prefix+index).getBytes())).header("Authorization",auth(u))).andExpect(status().isOk());
                    return "done";
                }));
            }
            gate.countDown(); for(var f:futures) f.get(15,java.util.concurrent.TimeUnit.SECONDS);
        }
        try(var files=Files.list(storage.resolve("pdfs"))) {
            var matches=files.filter(p -> {
                try { return Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS) && Files.readString(p).contains(prefix); }
                catch(java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
            }).toList();
            assertThat(matches).containsExactly(storage.resolve("pdfs").resolve(profile(u).getCvStoredName()));
        }
    }
    @Test void tradeCandidatesCanMaintainPhotosAndBuiltCvs() throws Exception {
        var u = candidate(); var c = profile(u); c.setCandidateType(CandidateType.TRADE); candidates.saveAndFlush(c);
        upload(u, image("png"), "trade.png", "image/png").andExpect(status().isOk());
        save(u, cv()).andExpect(status().isOk());
    }
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Test void rolledBackPhotoReplacementKeepsPreviousFileAndCleansNewFile() throws Exception {
        var u = candidate(); upload(u, image("png"), "a.png", "image/png").andExpect(status().isOk());
        String original = profile(u).getPhotoStoredName();
        var replacement = new java.util.concurrent.atomic.AtomicReference<String>();
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx -> {
            try {
                upload(u, image("jpeg"), "b.jpg", "image/jpeg").andExpect(status().isOk());
                replacement.set(profile(u).getPhotoStoredName());
                tx.setRollbackOnly();
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        assertThat(profile(u).getPhotoStoredName()).isEqualTo(original);
        assertThat(storage.resolve("photos").resolve(original)).exists();
        assertThat(storage.resolve("photos").resolve(replacement.get())).doesNotExist();
    }
    @Test void photoRetrievalDoesNotFollowSymlinks() throws Exception {
        var u = candidate(); var c = profile(u);
        String stored = UUID.randomUUID() + ".png";
        Files.createDirectories(storage.resolve("photos"));
        var target = Files.writeString(storage.resolve("private.txt"), "PRIVATE_DOCUMENT");
        Files.createSymbolicLink(storage.resolve("photos").resolve(stored), target);
        c.setPhotoStoredName(stored); candidates.saveAndFlush(c);
        mvc.perform(get("/api/candidates/" + c.getId() + "/photo")).andExpect(status().isNotFound());
    }

}
