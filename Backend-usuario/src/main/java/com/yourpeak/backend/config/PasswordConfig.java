package com.yourpeak.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.util.Map;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        Pbkdf2PasswordEncoder encoder = new Pbkdf2PasswordEncoder(
                "",
                16,
                600_000,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm
                        .PBKDF2WithHmacSHA256
        );

        String identificador = "pbkdf2-sha256-600k";

        return new DelegatingPasswordEncoder(
                identificador,
                Map.of(identificador, encoder)
        );
    }
}