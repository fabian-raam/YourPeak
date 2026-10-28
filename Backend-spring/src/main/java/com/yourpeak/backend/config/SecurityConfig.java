package com.yourpeak.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import jakarta.servlet.DispatcherType;

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
        var listarPendientes = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.GET, "/api/admin/clientes/pendientes");
        var obtenerPendiente = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.GET, "/api/admin/clientes/pendientes/{idCliente}");
        var listarPlanes = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.GET, "/api/admin/planes-membresia");
        var obtenerPlan = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.GET, "/api/admin/planes-membresia/{idPlan}");
        var crearPlan = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/admin/planes-membresia");
        var actualizarPlan = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.PUT, "/api/admin/planes-membresia/{idPlan}");
        var cambiarEstadoPlan = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.PATCH, "/api/admin/planes-membresia/{idPlan}/estado");

        http
                // Esta API no usa cookies ni sesión para autenticar solicitudes.
                // Las operaciones internas se protegen con la API key y la validación
                // del administrador en sus servicios.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Permite que Spring complete el despacho interno de errores
                        // y conserve el código HTTP real (por ejemplo, 409 Conflict).
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                registro,
                                login,
                                usuarioInterno,
                                listarPendientes,
                                obtenerPendiente,
                                listarPlanes,
                                obtenerPlan,
                                crearPlan,
                                actualizarPlan,
                                cambiarEstadoPlan
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/clientes/*/aprobar").permitAll()
                        .anyRequest().denyAll()
                );

        return http.build();
    }
}
