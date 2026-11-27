package com.yourpeak.backend.controller;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yourpeak.backend.dto.GoogleAuthRequest;
import com.yourpeak.backend.dto.LoginResponse;
import com.yourpeak.backend.service.GoogleAuthService;
import com.yourpeak.backend.service.GoogleMobileTokenVerifier;
import com.yourpeak.backend.service.GoogleMobileTokenVerifier.Identity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/google/mobile")
public class GoogleMobileAuthController {
    private static final String FLOW_KEY = "[A-Za-z0-9_-]{43}";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GoogleMobileTokenVerifier tokens;
    private final GoogleAuthService accounts;
    private final Cache<String, Boolean> challenges = temporaryCache();
    private final Cache<String, PendingAccount> pendingAccounts = temporaryCache();

    public GoogleMobileAuthController(GoogleMobileTokenVerifier tokens, GoogleAuthService accounts) {
        this.tokens = tokens;
        this.accounts = accounts;
    }

    @PostMapping("/challenge")
    public Challenge challenge() {
        String clientId = tokens.clientId();
        String nonce = newKey();
        challenges.put(nonce, true);
        return new Challenge(clientId, nonce);
    }

    @PostMapping("/login")
    public Result login(@Valid @RequestBody TokenRequest request) {
        if (challenges.asMap().remove(request.nonce()) == null) {
            throw expired();
        }
        Identity identity = tokens.verify(request.credential(), request.nonce());
        var result = accounts.authenticate(new GoogleAuthRequest(identity.subject(), identity.correo(),
                identity.nombre(), identity.apellido(), null, null));
        if (result.usuario() != null) return authenticated(result.usuario());

        String flowId = newKey();
        pendingAccounts.put(flowId, new PendingAccount(identity, "vincular".equals(result.estado())));
        return new Result(result.estado(), null, flowId,
                new Profile(identity.nombre(), identity.apellido(), identity.correo()));
    }

    @PostMapping("/complete")
    public Result complete(@Valid @RequestBody CompleteRequest request) {
        PendingAccount pending = pendingAccounts.getIfPresent(request.flowId());
        if (pending == null) throw expired();
        synchronized (pending) {
            if (pendingAccounts.getIfPresent(request.flowId()) != pending) throw expired();

            Identity identity = pending.identity;
            GoogleAuthRequest accountRequest = pending.link
                    ? linkRequest(request, pending)
                    : registrationRequest(request, identity);
            var result = accounts.authenticate(accountRequest);
            if (result.usuario() == null) {
                pendingAccounts.invalidate(request.flowId());
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta cambió. Vuelve a elegirla en Google.");
            }
            pendingAccounts.invalidate(request.flowId());
            return authenticated(result.usuario());
        }
    }

    private GoogleAuthRequest linkRequest(CompleteRequest request, PendingAccount pending) {
        if (request.contrasena() == null || request.contrasena().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa tu contraseña de YourPeak.");
        }
        if (pending.attempts >= 5) {
            pendingAccounts.invalidate(request.flowId());
            throw expired();
        }
        pending.attempts++;
        Identity identity = pending.identity;
        return new GoogleAuthRequest(identity.subject(), identity.correo(), null, null, null, request.contrasena());
    }

    private static GoogleAuthRequest registrationRequest(CompleteRequest request, Identity identity) {
        if (request.fechaNacimiento() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa tu fecha de nacimiento.");
        }
        return new GoogleAuthRequest(identity.subject(), identity.correo(), request.nombre(), request.apellido(),
                request.fechaNacimiento(), null);
    }

    private static Result authenticated(LoginResponse user) {
        return new Result("autenticado", user, null, null);
    }

    private static ResponseStatusException expired() {
        return new ResponseStatusException(HttpStatus.GONE, "La solicitud de Google venció. Vuelve a elegir tu cuenta.");
    }

    private static <T> Cache<String, T> temporaryCache() {
        return Caffeine.newBuilder().maximumSize(4096).expireAfterWrite(Duration.ofMinutes(10)).build();
    }

    private static String newKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static final class PendingAccount {
        private final Identity identity;
        private final boolean link;
        private int attempts;

        private PendingAccount(Identity identity, boolean link) {
            this.identity = identity;
            this.link = link;
        }
    }

    public record Challenge(String clientId, String nonce) {}
    public record Profile(String nombre, String apellido, String correo) {}
    public record Result(String estado, LoginResponse usuario, String flowId, Profile perfil) {}

    public record TokenRequest(@NotBlank @Pattern(regexp = FLOW_KEY) String nonce,
                               @NotBlank @Size(max = 8192) String credential) {
        @Override public String toString() { return "TokenRequest[datos ocultos]"; }
    }

    public record CompleteRequest(@NotBlank @Pattern(regexp = FLOW_KEY) String flowId,
                                  @Size(max = 100) String nombre, @Size(max = 100) String apellido,
                                  @Past LocalDate fechaNacimiento, @Size(max = 128) String contrasena) {
        @Override public String toString() { return "CompleteRequest[datos ocultos]"; }
    }
}
