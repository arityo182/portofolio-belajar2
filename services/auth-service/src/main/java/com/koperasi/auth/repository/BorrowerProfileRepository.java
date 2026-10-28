package com.koperasi.auth.repository;

import com.koperasi.auth.domain.entity.BorrowerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BorrowerProfileRepository extends JpaRepository<BorrowerProfile, UUID> {

    Optional<BorrowerProfile> findByUserId(UUID userId);
}
