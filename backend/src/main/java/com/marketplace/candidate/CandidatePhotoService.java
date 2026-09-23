package com.marketplace.candidate;

import com.marketplace.common.api.ApiException;
import com.marketplace.user.AccountStatus;
import com.marketplace.user.Role;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CandidatePhotoService {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000;
    private final CandidateService profiles;
    private final CandidateProfileRepository candidates;
    private final Path root;

    public CandidatePhotoService(CandidateService profiles, CandidateProfileRepository candidates,
            @Value("${app.photo.directory:uploads/photos}") String directory) {
        this.profiles = profiles; this.candidates = candidates;
        root = Path.of(directory).toAbsolutePath().normalize();
    }
    public static String url(CandidateProfile c) {
        return c.getPhotoStoredName() == null ? null : "/api/candidates/" + c.getId() + "/photo";
    }
    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("hasRole('CANDIDATE')")
    public CandidateDtos.ProfileView upload(MultipartFile file) throws IOException {
        var c = candidates.findByIdForUpdate(profiles.own().getId()).orElseThrow(CandidateService::missing);
        if (file.isEmpty() || file.getSize() > MAX_BYTES) throw invalid();
        // Decode content, bound dimensions before allocating pixels, then re-encode to strip metadata/payloads.
        byte[] bytes = file.getBytes();
        BufferedImage image;
        String format;
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            var reader = readers.next();
            try {
                format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg")) throw invalid();
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > MAX_PIXELS) throw invalid();
                image = reader.read(0);
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException ex) { throw invalid(); }
        if (image == null) throw invalid();
        String stored = UUID.randomUUID() + (format.equals("jpeg") ? ".jpg" : ".png");
        String previous = c.getPhotoStoredName();
        Files.createDirectories(root);
        // Register cleanup before writing so partial writes and transaction failures are removed too.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                String remove = status == STATUS_COMMITTED ? previous : stored;
                if (remove != null) try { Files.deleteIfExists(path(remove)); }
                catch (IOException | ApiException ex) {
                    org.slf4j.LoggerFactory.getLogger(CandidatePhotoService.class).warn("Could not clean up profile photo");
                }
            }
        });
        try (var output = Files.newOutputStream(path(stored), StandardOpenOption.CREATE_NEW)) {
            if (!ImageIO.write(image, format, output)) throw invalid();
        }
        c.setPhotoStoredName(stored);
        candidates.saveAndFlush(c);
        return profiles.view(c);
    }
    public record Photo(byte[] bytes, String contentType) {}
    @Transactional(readOnly = true)
    public Photo read(Long id) throws IOException {
        var c = candidates.findById(id).filter(p -> p.getUser().getAccountStatus() == AccountStatus.ACTIVE
            && p.getUser().getRole() == Role.CANDIDATE).orElseThrow(CandidateService::missing);
        if (c.getPhotoStoredName() == null) throw CandidateService.missing();
        var path = path(c.getPhotoStoredName());
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw CandidateService.missing();
        return new Photo(Files.readAllBytes(path), c.getPhotoStoredName().endsWith(".png") ? "image/png" : "image/jpeg");
    }
    private Path path(String name) {
        if (!name.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.(jpg|png)"))
            throw CandidateService.missing();
        return root.resolve(name);
    }
    private ApiException invalid() {
        return new ApiException(400, "INVALID_PHOTO", "Choose a valid JPEG or PNG up to 5 MB and 16 megapixels.");
    }
}
