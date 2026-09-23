package com.marketplace.candidate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import static com.marketplace.candidate.CandidateDtos.*;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateController {
    private final CandidateService service;
    private final CvService cvs;
    @GetMapping("/me") public ProfileView own() { return service.profile(); }
    @PutMapping("/me") public ProfileView update(@Valid @RequestBody ProfileRequest r) { return service.update(r); }
    @PostMapping("/me/skills") public ProfileView addSkill(@Valid @RequestBody SkillRequest r) { return service.addSkill(r); }
    @DeleteMapping("/me/skills/{id}") public ProfileView removeSkill(@PathVariable Long id) { return service.removeSkill(id); }
    @PostMapping(value="/me/cv", consumes="multipart/form-data")
    public ProfileView upload(@RequestParam("file") MultipartFile file) throws IOException { return cvs.upload(file); }
    @GetMapping("/me/cv") public ResponseEntity<byte[]> ownCv() throws IOException { return attachment(cvs.download(null)); }
    @GetMapping("/{id}/cv") public ResponseEntity<byte[]> cv(@PathVariable Long id) throws IOException { return attachment(cvs.download(id)); }
    private ResponseEntity<byte[]> attachment(byte[] bytes) {
        return ResponseEntity.ok().header("Content-Type","application/pdf").header("Content-Disposition","attachment; filename=resume.pdf")
            .header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").body(bytes);
    }
    @GetMapping("/{id}") public CandidateCard applicant(@PathVariable Long id) { return service.applicant(id); }
    @GetMapping public SearchPage search(@RequestParam(required=false) CandidateType candidateType,
        @RequestParam(required=false) String location, @RequestParam(required=false) Long skillId,
        @RequestParam(required=false) Availability availability, @RequestParam(defaultValue="0") int page) {
        return service.search(candidateType,location,skillId,availability,page);
    }
}
