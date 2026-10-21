package com.yourpeak.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) {

        var registro = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/auth/registro");
        var login = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/auth/login");
        var usuarioInterno = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.GET, "/api/auth/usuarios/{idUsuario}");

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(registro, login)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(registro, login, usuarioInterno).permitAll()
                        .anyRequest().denyAll()
                );

        return http.build();
    }
}
