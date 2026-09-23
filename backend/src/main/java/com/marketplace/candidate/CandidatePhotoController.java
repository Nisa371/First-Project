package com.marketplace.candidate;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/candidates")
public class CandidatePhotoController {
    private final CandidatePhotoService photos;
    @PostMapping(value = "/me/photo", consumes = "multipart/form-data")
    public CandidateDtos.ProfileView upload(@RequestParam("file") MultipartFile file) throws IOException {
        return photos.upload(file);
    }
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) throws IOException {
        var photo = photos.read(id);
        return ResponseEntity.ok().header("Content-Type", photo.contentType())
            .header("Cache-Control", "no-store").header("X-Content-Type-Options", "nosniff").body(photo.bytes());
    }
}
