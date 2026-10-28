package com.koperasi.auth.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "borrower_profiles", schema = "auth_schema")
@Getter
@Setter
@NoArgsConstructor
public class BorrowerProfile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(name = "nik_encrypted", columnDefinition = "TEXT")
    private String nikEncrypted;

    @Column(name = "npwp_encrypted", columnDefinition = "TEXT")
    private String npwpEncrypted;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 1)
    private String gender;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_encrypted", columnDefinition = "TEXT")
    private String bankAccountEncrypted;

    @Column(name = "income_monthly", precision = 15, scale = 2)
    private BigDecimal incomeMonthly;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "business_name", length = 255)
    private String businessName;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "ktp_photo_url", columnDefinition = "TEXT")
    private String ktpPhotoUrl;

    @Column(name = "selfie_photo_url", columnDefinition = "TEXT")
    private String selfiePhotoUrl;

    @Column(name = "ktp_photo_data")
    private byte[] ktpPhotoData;

    @Column(name = "selfie_photo_data")
    private byte[] selfiePhotoData;

    @Column(name = "ktp_photo_content_type", length = 50)
    private String ktpPhotoContentType;

    @Column(name = "selfie_photo_content_type", length = 50)
    private String selfiePhotoContentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static BorrowerProfile create(UUID userId) {
        BorrowerProfile profile = new BorrowerProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(userId);
        profile.setVerified(false);
        return profile;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
