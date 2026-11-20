package com.finsight.authentication.application;

import com.finsight.authentication.api.LoginRequest;
import com.finsight.authentication.api.RefreshRequest;
import com.finsight.authentication.api.RegisterRequest;
import com.finsight.authentication.api.TokenResponse;
import com.finsight.authentication.domain.RefreshTokenEntity;
import com.finsight.authentication.persistence.RefreshTokenRepository;
import com.finsight.common.error.ApiException;
import com.finsight.user.persistence.RoleEntity;
import com.finsight.user.persistence.RoleRepository;
import com.finsight.user.persistence.UserEntity;
import com.finsight.user.persistence.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final String USER_ROLE = "USER";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final SecurityProperties securityProperties;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthenticationService(UserRepository userRepository, RoleRepository roleRepository,
                                 RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
                                 AccessTokenService accessTokenService, SecurityProperties securityProperties,
                                 Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.securityProperties = securityProperties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        validatePasswordLength(request.password());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account already exists for this email");
        }
        RoleEntity role = roleRepository.findByName(USER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Required USER role is missing from the database"));
        Instant now = clock.instant();
        UserEntity user = new UserEntity(UUID.randomUUID().toString(), email,
                passwordEncoder.encode(request.password()), request.displayName().trim(), now, now, role);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account already exists for this email");
        }
        log.info("Account registration completed");
        return issueTokenPair(user, now);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        if (!isPasswordWithinBcryptLimit(request.password())) {
            passwordEncoder.matches("invalid", dummyPasswordHash);
            log.debug("Login rejected reason=invalid_credentials");
            throw invalidCredentials();
        }
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        String hash = user == null ? dummyPasswordHash : user.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user == null || !user.isEnabled() || !passwordMatches) {
            log.debug("Login rejected reason=invalid_credentials");
            throw invalidCredentials();
        }
        log.info("User login succeeded");
        return issueTokenPair(user, clock.instant());
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        Instant now = clock.instant();
        String tokenHash = hashToken(request.refreshToken());
        RefreshTokenEntity current = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(this::invalidRefreshToken);
        if (!current.isActiveAt(now)) {
            throw invalidRefreshToken();
        }
        UserEntity user = current.getUser();
        if (!user.isEnabled()) {
            throw invalidRefreshToken();
        }
        String replacement = newRefreshTokenValue();
        String replacementHash = hashToken(replacement);
        current.revoke(now, replacementHash);
        refreshTokenRepository.save(current);
        refreshTokenRepository.save(new RefreshTokenEntity(UUID.randomUUID().toString(), user,
                replacementHash, now.plus(securityProperties.refreshTokenTtl()), now));
        log.debug("Refresh token rotated");
        return new TokenResponse("Bearer", accessTokenService.issue(user.getId(), roleNames(user)),
                replacement, accessTokenService.expiresInSeconds(), now);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHashForUpdate(tokenHash).ifPresent(token -> {
            if (token.isActiveAt(clock.instant())) {
                token.revoke(clock.instant(), null);
                refreshTokenRepository.save(token);
            }
        });
    }

    private TokenResponse issueTokenPair(UserEntity user, Instant now) {
        String rawRefreshToken = newRefreshTokenValue();
        RefreshTokenEntity refreshToken = new RefreshTokenEntity(UUID.randomUUID().toString(), user,
                hashToken(rawRefreshToken), now.plus(securityProperties.refreshTokenTtl()), now);
        refreshTokenRepository.save(refreshToken);
        String accessToken = accessTokenService.issue(user.getId(), roleNames(user));
        return new TokenResponse("Bearer", accessToken, rawRefreshToken,
                accessTokenService.expiresInSeconds(), now);
    }

    private List<String> roleNames(UserEntity user) {
        return user.getRoles().stream().map(RoleEntity::getName).sorted().toList();
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect");
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePasswordLength(String password) {
        if (!isPasswordWithinBcryptLimit(password)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_LONG",
                    "Password must not exceed 72 UTF-8 bytes");
        }
    }

    private boolean isPasswordWithinBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private String newRefreshTokenValue() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
