package com.koperasi.auth.controller;

import com.koperasi.auth.domain.dto.ApiResponse;
import com.koperasi.auth.domain.dto.BorrowerProfileRequest;
import com.koperasi.auth.domain.dto.BorrowerProfileResponse;
import com.koperasi.auth.domain.dto.KtpVerificationResponse;
import com.koperasi.auth.exception.ForbiddenOperationException;
import com.koperasi.auth.security.JwtPrincipal;
import com.koperasi.auth.service.BorrowerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/borrowers")
@RequiredArgsConstructor
public class BorrowerProfileController {

    private final BorrowerProfileService profileService;

    @PostMapping("/profile")
    @PreAuthorize("hasRole('BORROWER')")
    public ResponseEntity<ApiResponse<BorrowerProfileResponse>> upsert(
            @Valid @RequestBody BorrowerProfileRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.upsert(principal.userId(), request),
                "Profil tersimpan, data sensitif dienkripsi"));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('BORROWER')")
    public ResponseEntity<ApiResponse<BorrowerProfileResponse>> get(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.get(principal.userId()), "OK"));
    }

    @PostMapping(value = "/profile/verify-ktp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('BORROWER')")
    public ResponseEntity<ApiResponse<KtpVerificationResponse>> verifyKtp(
            @RequestParam("ktpPhoto") MultipartFile ktpPhoto,
            @RequestParam("selfiePhoto") MultipartFile selfiePhoto,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.accepted().body(ApiResponse.ok(
                profileService.uploadDocuments(principal.userId(), ktpPhoto, selfiePhoto),
                "Dokumen diunggah, menunggu verifikasi"));
    }

    @GetMapping("/profile/documents/{kind}")
    @PreAuthorize("hasRole('BORROWER')")
    public ResponseEntity<byte[]> getOwnDocument(
            @PathVariable String kind,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return documentResponse(profileService.getDocument(principal.userId(), kind));
    }

    @GetMapping("/{userId}/profile")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public ResponseEntity<ApiResponse<BorrowerProfileResponse>> getByUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.getForUser(principal.userId(), principal.roles(), userId), "OK"));
    }

    @PostMapping("/{userId}/verify")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public ResponseEntity<ApiResponse<BorrowerProfileResponse>> verify(
            @PathVariable UUID userId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.verify(userId, principal.userId()),
                "Dokumen borrower berhasil diverifikasi"));
    }

    @GetMapping("/{userId}/documents/{kind}")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public ResponseEntity<byte[]> getUserDocument(
            @PathVariable UUID userId,
            @PathVariable String kind,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ensurePrivileged(principal);
        return documentResponse(profileService.getDocument(userId, kind));
    }

    private ResponseEntity<byte[]> documentResponse(BorrowerProfileService.Document document) {
        if (document.content() == null || document.content().length == 0) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = document.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(document.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("Content-Disposition", "inline; filename=\"" + document.name() + "\"")
                .body(document.content());
    }

    private void ensurePrivileged(JwtPrincipal principal) {
        Set<String> roles = principal.roles() == null ? Set.of() : principal.roles();
        if (roles.stream().noneMatch(role -> Set.of("REVIEWER", "ADMIN").contains(role))) {
            throw new ForbiddenOperationException("Anda tidak memiliki akses ke dokumen ini");
        }
    }
}
