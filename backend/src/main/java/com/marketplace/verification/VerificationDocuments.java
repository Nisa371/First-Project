package com.marketplace.verification;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.CandidateProfileRepository;
import com.marketplace.employer.EmployerProfileRepository;
import com.marketplace.common.api.ApiException;
import com.marketplace.user.*;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service @Transactional
public class VerificationDocuments {
    private final CurrentAccount current;
    private final UserRepository users;
    private final CandidateProfileRepository candidates;
    private final EmployerProfileRepository employers;
    private final VerificationRecordRepository records;
    private final VerificationChecklist checklist;
    private final Path root;
    public VerificationDocuments(CurrentAccount current, UserRepository users, CandidateProfileRepository candidates,
        EmployerProfileRepository employers, VerificationRecordRepository records, VerificationChecklist checklist,
        @Value("${app.verification.directory:uploads/verification}") String directory) {
        this.current=current;this.users=users;this.candidates=candidates;this.employers=employers;this.records=records;this.checklist=checklist;
        root=Path.of(directory).toAbsolutePath().normalize();
    }
    public record Decision(@NotNull VerificationStatus status, @Size(max=3000) String notes) {}
    public record Review(Long id, String ownerName, Role role, Long companyTypeId, String companyTypeName,
        String requirementName, VerificationStatus status, String reviewNote, String identityReference,
        Instant submittedAt, Instant reviewedAt, Long reviewerId, boolean hasFile, boolean latest, boolean applicable) {}
    public record Download(byte[] bytes, String contentType) {}
    @PreAuthorize("hasAnyRole('CANDIDATE','EMPLOYER')")
    public VerificationChecklist.Checklist own() { return checklist.forUser(current.requireActive()); }
    @PreAuthorize("hasAnyRole('CANDIDATE','EMPLOYER')")
    @Transactional(rollbackFor=IOException.class)
    public VerificationChecklist.Document upload(Long requirementId, MultipartFile file) throws IOException {
        var owner=users.findByIdForUpdate(current.requireActive().getId()).orElseThrow(VerificationChecklist::missing);
        var requirement=checklist.applicable(owner).stream().filter(r -> r.getId().equals(requirementId)).findFirst()
            .orElseThrow(() -> new ApiException(400,"INVALID_REQUIREMENT","This requirement does not apply to your account."));
        String name=file.getOriginalFilename(), mime=file.getContentType();
        if(file.isEmpty() || file.getSize()>5*1024*1024 || name==null || name.length()>180 || name.isBlank()
            || name.contains("/") || name.contains("\\") || name.chars().anyMatch(c -> c<32 || c==127)) throw invalidFile();
        byte[] bytes=file.getBytes(); String ext;
        String lower=name.toLowerCase(Locale.ROOT);
        if("application/pdf".equals(mime) && lower.endsWith(".pdf") && bytes.length>=8 && new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-")) ext=".pdf";
        else if("image/png".equals(mime) && lower.endsWith(".png") && bytes.length>=8 && Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10})) ext=".png";
        else if("image/jpeg".equals(mime) && (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) && bytes.length>=3 && bytes[0]==(byte)255 && bytes[1]==(byte)216 && bytes[2]==(byte)255) ext=".jpg";
        else throw invalidFile();
        var record=new VerificationRecord();record.setOwner(owner);record.setRequirement(requirement);
        if(owner.getRole()==Role.CANDIDATE) record.setCandidate(candidates.findByUserId(owner.getId()).orElseThrow(VerificationChecklist::missing));
        String stored=UUID.randomUUID()+ext;
        Files.createDirectories(root);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if(status!=STATUS_COMMITTED) try { Files.deleteIfExists(path(stored)); }
                catch(IOException e) { org.slf4j.LoggerFactory.getLogger(VerificationDocuments.class).warn("Could not remove rolled-back verification upload"); }
            }
        });
        Files.write(path(stored),bytes,StandardOpenOption.CREATE_NEW);
        record.setStoredName(stored);record.setOriginalName(name);record.setContentType(mime);record.setFileSize(file.getSize());
        // Replacements are new submissions; older files and decisions remain private history.
        return checklist.document(records.saveAndFlush(record));
    }
    @PreAuthorize("hasAnyRole('CANDIDATE','EMPLOYER','ADMIN')")
    @Transactional(readOnly=true)
    public Download download(Long id) throws IOException {
        var actor=current.requireActive();var record=records.findById(id).orElseThrow(VerificationChecklist::missing);
        if(actor.getRole()!=Role.ADMIN && !actor.getId().equals(checklist.owner(record).getId()))
            throw new ApiException(403,"FORBIDDEN","You cannot access another account's verification document.");
        if(record.getStoredName()==null || !Files.isRegularFile(path(record.getStoredName()),LinkOption.NOFOLLOW_LINKS)) throw VerificationChecklist.missing();
        return new Download(Files.readAllBytes(path(record.getStoredName())),record.getContentType());
    }
    @PreAuthorize("hasRole('ADMIN')")
    public List<Review> reviews() {
        current.requireActive();return records.findAll(org.springframework.data.domain.PageRequest.of(0,50,org.springframework.data.domain.Sort.by("submittedAt","id").descending())).stream().map(this::view).toList();
    }
    public record ReviewPage(List<Review> content,long totalElements,int page,int totalPages) {}
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewPage reviewPage(String status,String role,Long companyId,int page,int size) {
        current.requireActive();
        if(page<0 || size<1 || size>50 || (long)page*size>Integer.MAX_VALUE || !List.of("","PENDING","VERIFIED","FAILED").contains(status) || !List.of("","CANDIDATE","EMPLOYER").contains(role))
            throw new ApiException(400,"INVALID_FILTER","Select valid review filters and a page size of 1–50.");
        var result=records.reviewPage(status,role,companyId,org.springframework.data.domain.PageRequest.of(page,size,org.springframework.data.domain.Sort.by("submittedAt","id").descending()));
        return new ReviewPage(result.stream().map(this::view).toList(),result.getTotalElements(),page,result.getTotalPages());
    }
    @PreAuthorize("hasRole('ADMIN')")
    public Review review(Long id, Decision decision) {
        var actor=current.requireActive();
        if(!List.of(VerificationStatus.VERIFIED,VerificationStatus.FAILED).contains(decision.status()))
            throw new ApiException(400,"INVALID_DECISION","Choose approved or rejected.");
        if(decision.status()==VerificationStatus.FAILED && (decision.notes()==null || decision.notes().isBlank()))
            throw new ApiException(400,"VALIDATION_ERROR","Explain why this document was rejected.");
        Long ownerId=records.ownerId(id).orElseThrow(VerificationChecklist::missing);
        // Same lock order as upload prevents reviewing an upload while it is being superseded.
        users.findByIdForUpdate(ownerId).orElseThrow(VerificationChecklist::missing);
        var record=records.findByIdForUpdate(id).orElseThrow(VerificationChecklist::missing);
        if(!checklist.latest(record) || !List.of(VerificationStatus.PENDING,VerificationStatus.IN_REVIEW).contains(record.getStatus()))
            throw new ApiException(409,"FINAL_REVIEW","This submission was already reviewed or replaced. Refresh the list.");
        if(record.getRequirement()!=null && checklist.applicable(checklist.owner(record)).stream().noneMatch(r -> r.getId().equals(record.getRequirement().getId())))
            throw new ApiException(409,"REQUIREMENT_CHANGED","This requirement no longer applies. The submission remains in history.");
        record.setStatus(decision.status());record.setReviewerNotes(decision.notes()==null?null:decision.notes().strip());record.setReviewerUser(actor);record.setReviewedAt(Instant.now());
        return view(record);
    }
    private Review view(VerificationRecord v) {
        var user=checklist.owner(v);var employer=user.getRole()==Role.EMPLOYER?employers.findByUserId(user.getId()).orElseThrow(VerificationChecklist::missing):null;
        var type=employer==null?null:employer.getCompanyType();
        String name=employer!=null?employer.getCompanyName():v.getCandidate().getFullName();
        boolean applicable=checklist.applicable(user).stream().anyMatch(r -> checklist.matches(v,r));
        return new Review(v.getId(),name,user.getRole(),type==null?null:type.getId(),type==null?null:type.getName(),
            v.getRequirement()==null?"National ID (legacy)":v.getRequirement().getName(),v.getStatus(),v.getReviewerNotes(),v.getStoredName()==null?v.getIdentityReference():null,
            v.getSubmittedAt(),v.getReviewedAt(),v.getReviewerUser()==null?null:v.getReviewerUser().getId(),v.getStoredName()!=null,checklist.latest(v),applicable);
    }
    private Path path(String name) {
        if(!name.matches("[a-f0-9-]{36}\\.(pdf|png|jpg)")) throw VerificationChecklist.missing();
        Path target=root.resolve(name).normalize();
        if(!root.equals(target.getParent())) throw VerificationChecklist.missing();
        return target;
    }
    private static ApiException invalidFile() { return new ApiException(400,"INVALID_DOCUMENT","Choose a valid PDF, JPG or PNG up to 5 MB with a simple filename."); }
}
