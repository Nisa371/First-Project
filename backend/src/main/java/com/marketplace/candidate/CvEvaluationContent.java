package com.marketplace.candidate;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CvEvaluationContent {
    private final CandidateCvRepository cvs;
    private final Path root;
    public CvEvaluationContent(CandidateCvRepository cvs,
            @Value("${app.cv.directory:uploads/cv}") String directory) {
        this.cvs = cvs;
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }
    public record Content(StructuredCvDtos.Content structured, String uploadedPdfText) {
        public boolean usable() { return structured != null || uploadedPdfText != null; }
    }
    public Content forCandidate(CandidateProfile candidate) {
        var structured = cvs.findByCandidateId(candidate.getId()).filter(CandidateCv::hasContent)
            .map(StructuredCvService::content).orElse(null);
        String pdf = null;
        if (candidate.getCvStoredName() != null) {
            try {
                var path = root.resolve(candidate.getCvStoredName()).normalize();
                if (root.equals(path.getParent()) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                        && Files.size(path) <= 5 * 1024 * 1024)
                    pdf = extract(Files.readAllBytes(path));
            } catch (IOException | RuntimeException ignored) { /* Safe unavailable result, never expose file details. */ }
        }
        return new Content(structured, pdf);
    }
    public String extract(byte[] bytes) {
        try (var document = Loader.loadPDF(bytes)) {
            if (!document.getCurrentAccessPermission().canExtractContent() || document.getNumberOfPages() > 50)
                return null;
            var text = new StringBuilder();
            var stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            // Bound extracted output even for small PDFs with unusually large text streams.
            stripper.writeText(document, new Writer() {
                public void write(char[] chars, int offset, int length) throws IOException {
                    if (text.length() + length > 100_000) throw new IOException("CV text limit exceeded");
                    text.append(chars, offset, length);
                }
                public void flush() { }
                public void close() { }
            });
            String normalized = text.toString().replaceAll("[\\p{Cc}&&[^\\n\\t]]", " ").strip();
            return normalized.codePoints().filter(Character::isLetterOrDigit).limit(40).count() >= 40
                ? normalized : null;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }
}
