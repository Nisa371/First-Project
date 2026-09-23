package com.marketplace.verification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import java.io.IOException;
import java.util.List;
@RestController @RequiredArgsConstructor
public class VerificationDocumentController {
    private final VerificationDocuments documents;
    private final VerificationRequirements requirements;
    @GetMapping("/api/verifications/me/checklist")
    public VerificationChecklist.Checklist checklist() { return documents.own(); }
    @PostMapping(value="/api/verifications/me/requirements/{id}/document", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public VerificationChecklist.Document upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException { return documents.upload(id,file); }
    @GetMapping("/api/verifications/documents/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) throws IOException {
        var file=documents.download(id);
        String ext=switch(file.contentType()) { case "image/png" -> "png"; case "image/jpeg" -> "jpg"; default -> "pdf"; };
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"verification-"+id+"."+ext+"\"")
            .header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff").body(file.bytes());
    }
    @GetMapping("/api/admin/verification-requirements") public List<VerificationRequirements.View> requirements() { return requirements.all(); }
    @PostMapping("/api/admin/verification-requirements") @ResponseStatus(HttpStatus.CREATED)
    public VerificationRequirements.View create(@Valid @RequestBody VerificationRequirements.Input input) { return requirements.create(input); }
    @PutMapping("/api/admin/verification-requirements/{id}")
    public VerificationRequirements.View update(@PathVariable Long id, @Valid @RequestBody VerificationRequirements.Input input) { return requirements.update(id,input); }
    @GetMapping("/api/admin/verification-submissions") public List<VerificationDocuments.Review> reviews() { return documents.reviews(); }
    @GetMapping("/api/admin/verification-submissions/page")
    public VerificationDocuments.ReviewPage reviewPage(@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String role,@RequestParam(required=false) Long companyId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return documents.reviewPage(status,role,companyId,page,size); }
    @PostMapping("/api/admin/verification-submissions/{id}/review")
    public VerificationDocuments.Review review(@PathVariable Long id, @Valid @RequestBody VerificationDocuments.Decision decision) { return documents.review(id,decision); }
}
