package com.koperasi.auth.service;

import com.koperasi.auth.domain.dto.BorrowerProfileRequest;
import com.koperasi.auth.domain.dto.BorrowerProfileResponse;
import com.koperasi.auth.domain.dto.KtpVerificationResponse;
import com.koperasi.auth.domain.entity.BorrowerProfile;
import com.koperasi.auth.exception.ForbiddenOperationException;
import com.koperasi.auth.exception.InvalidDocumentException;
import com.koperasi.auth.exception.ResourceNotFoundException;
import com.koperasi.auth.repository.BorrowerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BorrowerProfileService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final BorrowerProfileRepository profileRepository;
    private final PiiEncryptionService encryptionService;

    @Transactional
    public BorrowerProfileResponse upsert(UUID userId, BorrowerProfileRequest request) {
        BorrowerProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> BorrowerProfile.create(userId));

        applySensitive(profile, "NIK", request.nik(), true, profile::setNikEncrypted);
        applySensitive(profile, "NPWP", request.npwp(), false, profile::setNpwpEncrypted);
        profile.setFullName(request.fullName().trim());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(request.gender().toUpperCase(Locale.ROOT));
        profile.setAddress(request.address().trim());
        profile.setCity(request.city().trim());
        profile.setProvince(request.province().trim());
        profile.setBankName(request.bankName().trim());
        applySensitive(profile, "Nomor rekening", request.bankAccount(), true,
                profile::setBankAccountEncrypted);
        profile.setIncomeMonthly(request.incomeMonthly());
        profile.setBusinessType(request.businessType().trim());
        profile.setBusinessName(request.businessName().trim());
        return toResponse(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public BorrowerProfileResponse get(UUID userId) {
        BorrowerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil borrower belum diisi"));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public BorrowerProfileResponse getForUser(UUID requestingUserId, Set<String> roles, UUID targetUserId) {
        boolean privileged = roles != null && roles.stream()
                .anyMatch(role -> Set.of("REVIEWER", "ADMIN").contains(role));
        if (!requestingUserId.equals(targetUserId) && !privileged) {
            throw new ForbiddenOperationException("Anda tidak memiliki akses ke profil ini");
        }
        return get(targetUserId);
    }

    @Transactional
    public KtpVerificationResponse uploadDocuments(UUID userId, MultipartFile ktpPhoto,
                                                    MultipartFile selfiePhoto) {
        byte[] ktpBytes = readDocument(ktpPhoto, "Foto KTP");
        byte[] selfieBytes = readDocument(selfiePhoto, "Foto selfie");

        BorrowerProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> BorrowerProfile.create(userId));
        profile.setKtpPhotoData(ktpBytes);
        profile.setKtpPhotoContentType(ktpPhoto.getContentType());
        profile.setKtpPhotoUrl("/api/v1/borrowers/profile/documents/ktp");
        profile.setSelfiePhotoData(selfieBytes);
        profile.setSelfiePhotoContentType(selfiePhoto.getContentType());
        profile.setSelfiePhotoUrl("/api/v1/borrowers/profile/documents/selfie");
        // A new document upload invalidates a previous verification until a reviewer
        // checks the replacement files.
        profile.setVerified(false);
        profile.setVerifiedAt(null);
        profile.setVerifiedBy(null);
        profile = profileRepository.save(profile);

        return new KtpVerificationResponse(profile.getId(), "PENDING",
                profile.getKtpPhotoUrl(), profile.getSelfiePhotoUrl());
    }

    @Transactional
    public BorrowerProfileResponse verify(UUID targetUserId, UUID verifierId) {
        BorrowerProfile profile = profileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil borrower belum diisi"));
        if (profile.getKtpPhotoData() == null || profile.getKtpPhotoData().length == 0
                || profile.getSelfiePhotoData() == null || profile.getSelfiePhotoData().length == 0) {
            throw new InvalidDocumentException(
                    "Foto KTP dan selfie harus diunggah sebelum verifikasi");
        }
        profile.setVerified(true);
        profile.setVerifiedAt(Instant.now());
        profile.setVerifiedBy(verifierId);
        return toResponse(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public Document getDocument(UUID userId, String kind) {
        BorrowerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil borrower belum diisi"));
        return switch (kind.toLowerCase(Locale.ROOT)) {
            case "ktp" -> new Document(profile.getKtpPhotoData(), profile.getKtpPhotoContentType(), "ktp");
            case "selfie" -> new Document(profile.getSelfiePhotoData(), profile.getSelfiePhotoContentType(), "selfie");
            default -> throw new ResourceNotFoundException("Jenis dokumen tidak dikenal: " + kind);
        };
    }

    private byte[] readDocument(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException(label + " wajib diunggah");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidDocumentException(label + " maksimal 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidDocumentException(label + " harus berupa JPEG atau PNG");
        }
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new InvalidDocumentException(label + " tidak dapat dibaca");
        }
    }

    private void applySensitive(BorrowerProfile profile, String label, String value,
                                boolean required, java.util.function.Consumer<String> setter) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            if (required && profile.getId() == null) {
                throw new InvalidDocumentException(label + " wajib diisi");
            }
            return;
        }
        if (normalized.contains("*")) {
            if (profile.getId() == null) {
                throw new InvalidDocumentException(label + " wajib diisi");
            }
            return;
        }
        setter.accept(encryptionService.encrypt(normalized));
    }

    private BorrowerProfileResponse toResponse(BorrowerProfile profile) {
        return new BorrowerProfileResponse(
                profile.getId(),
                profile.getFullName(),
                mask(profile.getNikEncrypted(), 6, 4),
                mask(profile.getNpwpEncrypted(), 2, 4),
                profile.getDateOfBirth() == null ? null : profile.getDateOfBirth().toString(),
                profile.getGender(),
                profile.getAddress(),
                profile.getCity(),
                profile.getProvince(),
                profile.getBankName(),
                mask(profile.getBankAccountEncrypted(), 0, 4),
                profile.getIncomeMonthly(),
                profile.getBusinessType(),
                profile.getBusinessName(),
                profile.isVerified(),
                profile.getVerifiedAt(),
                profile.getKtpPhotoUrl(),
                profile.getSelfiePhotoUrl(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private String mask(String encrypted, int prefixLength, int suffixLength) {
        String value;
        try {
            value = encryptionService.decrypt(encrypted);
        } catch (RuntimeException ex) {
            return null;
        }
        if (value == null || value.isBlank()) {
            return null;
        }
        int length = value.length();
        int prefix = Math.min(Math.max(0, prefixLength), length);
        int suffix = Math.min(Math.max(0, suffixLength), length - prefix);
        return value.substring(0, prefix)
                + "*".repeat(Math.max(0, length - prefix - suffix))
                + (suffix > 0 ? value.substring(length - suffix) : "");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record Document(byte[] content, String contentType, String name) {
    }
}
