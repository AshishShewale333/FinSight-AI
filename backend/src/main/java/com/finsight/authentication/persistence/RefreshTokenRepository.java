package com.finsight.authentication.persistence;

import com.finsight.authentication.domain.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshTokenEntity token where token.tokenHash = :hash")
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("hash") String hash);

    Optional<RefreshTokenEntity> findByTokenHash(String hash);

    @Modifying
    @Query("update RefreshTokenEntity token set token.revokedAt = :now where token.user.id = :userId and token.revokedAt is null")
    int revokeAllActiveByUserId(@Param("userId") String userId, @Param("now") Instant now);
}
