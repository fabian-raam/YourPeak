package com.yourpeak.backend.service;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoogleMobileTokenVerifierTests {
    @Test
    void acceptsVerifiedInstitutionalAccount() {
        var identity = GoogleMobileTokenVerifier.verifiedIdentity(token().build(), "web-client", "challenge");
        assertEquals("ana@tecsup.edu.pe", identity.correo());
        assertEquals("1234", identity.subject());
    }

    @Test
    void rejectsPersonalAndUnverifiedAccounts() {
        for (Jwt jwt : List.of(
                token().claim("hd", "gmail.com").build(),
                token().claim("email", "ana@tecsup.edu.pe.example.org").build(),
                token().claim("email_verified", false).build())) {
            assertEquals(HttpStatus.FORBIDDEN, failure(jwt).getStatusCode());
        }
    }

    @Test
    void rejectsWrongAudienceIssuerNonceAndExpiry() {
        for (Jwt jwt : List.of(
                token().audience(List.of("other")).build(),
                token().issuer("https://example.org").build(),
                token().claim("nonce", "other").build(),
                token().expiresAt(Instant.now().minusSeconds(1)).build())) {
            assertEquals(HttpStatus.UNAUTHORIZED, failure(jwt).getStatusCode());
        }
    }

    private static ResponseStatusException failure(Jwt jwt) {
        return assertThrows(ResponseStatusException.class,
                () -> GoogleMobileTokenVerifier.verifiedIdentity(jwt, "web-client", "challenge"));
    }

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("verified-by-decoder")
                .header("alg", "RS256")
                .issuer("https://accounts.google.com")
                .audience(List.of("web-client")).subject("1234")
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("nonce", "challenge").claim("hd", "tecsup.edu.pe")
                .claim("email", "Ana@tecsup.edu.pe").claim("email_verified", true);
    }
}
