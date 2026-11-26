package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.GoogleAuthRequest;
import com.yourpeak.backend.service.GoogleAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {
    private final GoogleAuthService service;
    private final String internalKey;

    public GoogleAuthController(GoogleAuthService service, @Value("${yourpeak.google-internal-key:}") String internalKey) {
        this.service = service;
        this.internalKey = internalKey;
    }

    @PostMapping
    public GoogleAuthService.Result authenticate(
            @RequestHeader(value = "X-Google-Internal-Key", required = false) String key,
            @Valid @RequestBody GoogleAuthRequest request
    ) {
        if (internalKey.length() < 32 || key == null || !MessageDigest.isEqual(
                internalKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso no autorizado.");
        }
        return service.authenticate(request);
    }
}
