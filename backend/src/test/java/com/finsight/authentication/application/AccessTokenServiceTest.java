package com.finsight.authentication.application;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessTokenServiceTest {

    private static final String ISSUER = "https://finsight-test.local";

    @Test
    void issuedTokenContainsSubjectIssuerAndRolesAndVerifies() {
        SecretKey key = new SecretKeySpec(new byte[32], "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
        AccessTokenService tokenService = new AccessTokenService(encoder,
                new SecurityProperties(Base64.getEncoder().encodeToString(new byte[32]), ISSUER,
                        Duration.ofMinutes(5), Duration.ofDays(30)), Clock.systemUTC());

        String token = tokenService.issue("user-123", List.of("USER"));
        JwtDecoder decoder = decoder(key);
        var decoded = decoder.decode(token);

        assertEquals("user-123", decoded.getSubject());
        assertEquals(ISSUER, decoded.getIssuer().toString());
        assertEquals(List.of("USER"), decoded.getClaimAsStringList("roles"));
        assertEquals(300, tokenService.expiresInSeconds());
    }

    @Test
    void decoderRejectsTamperedSignature() {
        SecretKey key = new SecretKeySpec(new byte[32], "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
        AccessTokenService tokenService = new AccessTokenService(encoder,
                new SecurityProperties(Base64.getEncoder().encodeToString(new byte[32]), ISSUER,
                        Duration.ofMinutes(5), Duration.ofDays(30)), Clock.systemUTC());
        String token = tokenService.issue("user-123", List.of("USER"));
        int separator = token.lastIndexOf('.');
        char replacement = token.charAt(separator + 1) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, separator + 1) + replacement + token.substring(separator + 2);

        assertThrows(RuntimeException.class, () -> decoder(key).decode(tampered));
    }

    private JwtDecoder decoder(SecretKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }
}
