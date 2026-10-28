package com.koperasi.auth.repository;

import com.koperasi.auth.domain.entity.User;
import com.koperasi.auth.domain.enums.RoleName;
import com.koperasi.auth.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<User> findWithRolesById(UUID id);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select u from User u
            where (lower(u.email) like lower(concat('%', coalesce(:search, ''), '%'))
                    or coalesce(u.phone, '') like concat('%', coalesce(:search, ''), '%'))
              and (:status is null or u.status = :status)
              and (:role is null or exists (
                    select 1 from UserRole ur
                    where ur.user = u and ur.role.name = :role
              ))
            """)
    Page<User> search(@Param("search") String search,
                      @Param("role") RoleName role,
                      @Param("status") UserStatus status,
                      Pageable pageable);
}
