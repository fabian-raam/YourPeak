package com.yourpeak.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GoogleMobileTokenVerifier {
    private static final String DOMAIN = "tecsup.edu.pe";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@tecsup\\.edu\\.pe$");
    private static final Pattern SUBJECT = Pattern.compile("[A-Za-z0-9_-]{1,255}");

    private final String clientId;
    private final JwtDecoder decoder;

    public GoogleMobileTokenVerifier(@Value("${yourpeak.google-oauth-client-id:}") String clientId) {
        this.clientId = clientId.strip();
        var transport = new SimpleClientHttpRequestFactory();
        transport.setConnectTimeout(Duration.ofSeconds(5));
        transport.setReadTimeout(Duration.ofSeconds(5));
        decoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .restOperations(new RestTemplate(transport))
                .build();
    }

    public String clientId() {
        if (clientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El acceso con Google no está configurado.");
        }
        return clientId;
    }

    public Identity verify(String credential, String nonce) {
        clientId();
        try {
            return verifiedIdentity(decoder.decode(credential), clientId, nonce);
        } catch (BadJwtException exception) {
            throw invalidCredential();
        } catch (JwtException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo verificar la cuenta con Google. Inténtalo de nuevo.");
        }
    }

    static Identity verifiedIdentity(Jwt token, String audience, String nonce) {
        String issuer = token.getClaimAsString("iss");
        Instant expiry = token.getExpiresAt();
        if (expiry == null || !expiry.isAfter(Instant.now()) || token.getAudience() == null
                || !token.getAudience().contains(audience)
                || !("https://accounts.google.com".equals(issuer) || "accounts.google.com".equals(issuer))
                || !nonce.equals(token.getClaimAsString("nonce"))) {
            throw invalidCredential();
        }
        String email = Objects.toString(token.getClaimAsString("email"), "").strip().toLowerCase(Locale.ROOT);
        if (!DOMAIN.equals(token.getClaimAsString("hd")) || !Boolean.TRUE.equals(token.getClaims().get("email_verified"))
                || email.length() > 254 || !EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usa tu cuenta institucional @tecsup.edu.pe.");
        }
        String subject = token.getSubject();
        if (subject == null || !SUBJECT.matcher(subject).matches()) {
            throw invalidCredential();
        }
        return new Identity(subject, email, name(token, "given_name"), name(token, "family_name"));
    }

    private static String name(Jwt token, String claim) {
        String value = Objects.toString(token.getClaimAsString(claim), "").strip();
        return value.length() <= 100 ? value : "";
    }

    private static ResponseStatusException invalidCredential() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Google no pudo verificar tu cuenta. Vuelve a elegirla.");
    }

    public record Identity(String subject, String correo, String nombre, String apellido) {}
}
