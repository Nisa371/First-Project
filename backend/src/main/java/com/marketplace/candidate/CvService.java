package com.marketplace.candidate;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.UUID;
import com.marketplace.common.api.ApiException;
import com.marketplace.auth.CurrentAccount;
import com.marketplace.user.Role;
import com.marketplace.user.AccountStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CvService {
    private final CandidateService profiles;
    private final CandidateProfileRepository candidates;
    private final CurrentAccount current;
    private final Path root;
    private final com.marketplace.job.JobApplicationRepository applications;
    public CvService(CandidateService profiles, CandidateProfileRepository candidates, CurrentAccount current, com.marketplace.job.JobApplicationRepository applications,
        @Value("${app.cv.directory:uploads/cv}") String directory) {
        this.applications=applications; this.profiles=profiles; this.candidates=candidates; this.current=current;
        this.root=Path.of(directory).toAbsolutePath().normalize();
    }
    @Transactional
    @PreAuthorize("hasRole('CANDIDATE')")
    public CandidateDtos.ProfileView upload(MultipartFile file) throws IOException {
        var c=profiles.own();
        if(c.getCandidateType()!=CandidateType.TECH) throw new ApiException(403,"TECH_ONLY","CV upload is available for TECH candidates.");
        String name=file.getOriginalFilename();
        if(file.isEmpty() || file.getSize()>5*1024*1024 || name==null || name.length()>180
            || name.contains("/") || name.contains("\\") || name.chars().anyMatch(ch -> ch<32)
            || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf") || !"application/pdf".equals(file.getContentType()))
            throw new ApiException(400,"INVALID_CV","Choose a PDF file up to 5 MB with a simple filename.");
        byte[] bytes=file.getBytes();
        if(bytes.length<8 || !new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))
            throw new ApiException(400,"INVALID_CV","The file must contain a PDF document.");
        String previous=c.getCvStoredName(); String stored=UUID.randomUUID()+".pdf";
        Files.createDirectories(root); Files.write(root.resolve(stored),bytes,StandardOpenOption.CREATE_NEW);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                String remove=status==STATUS_COMMITTED ? previous : stored;
                if(remove!=null) { try { Files.deleteIfExists(path(remove)); } catch(IOException ex) {
                    org.slf4j.LoggerFactory.getLogger(CvService.class).warn("Could not remove superseded CV file");
                } }
            }
        });
        c.setCvStoredName(stored); c.setCvOriginalName(name); c.setCvContentType("application/pdf");
        candidates.saveAndFlush(c); return profiles.view(c);
    }
    @Transactional(readOnly=true)
    @PreAuthorize("hasAnyRole('CANDIDATE','EMPLOYER')")
    public byte[] download(Long candidateId) throws IOException {
        var user=current.requireActive();
        var c=candidateId==null ? profiles.own() : candidates.findById(candidateId).orElseThrow(CandidateService::missing);
        if(user.getRole()==Role.CANDIDATE) current.requireOwner(c.getUser().getId());
        else if(!applications.existsByCandidateIdAndJobEmployerUserId(c.getId(),user.getId()) || c.getUser().getAccountStatus()!=AccountStatus.ACTIVE || c.getUser().getRole()!=Role.CANDIDATE)
            throw CandidateService.missing();
        if(c.getCvStoredName()==null || !Files.isRegularFile(path(c.getCvStoredName()))) throw CandidateService.missing();
        return Files.readAllBytes(path(c.getCvStoredName()));
    }
    private Path path(String name) {
        Path target=root.resolve(name).normalize();
        if(!target.getParent().equals(root)) throw CandidateService.missing();
        return target;
    }
}
