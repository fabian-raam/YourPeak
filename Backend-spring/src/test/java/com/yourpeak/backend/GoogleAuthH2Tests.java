package com.yourpeak.backend;

import com.yourpeak.backend.dto.GoogleAuthRequest;
import com.yourpeak.backend.dto.LoginRequest;
import com.yourpeak.backend.dto.RegistroRequest;
import com.yourpeak.backend.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import com.yourpeak.backend.service.GoogleAuthService;
import com.yourpeak.backend.service.GoogleMobileTokenVerifier;
import com.yourpeak.backend.service.GoogleMobileTokenVerifier.Identity;
import com.yourpeak.backend.service.LoginService;
import com.yourpeak.backend.service.RegistroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:acceso-google;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "yourpeak.google-internal-key=clave-interna-solo-pruebas-google"
})
@AutoConfigureMockMvc
@Transactional
class GoogleAuthH2Tests {
    private static final String EMAIL = "ana@tecsup.edu.pe";
    private static final String PASSWORD = "contraseña existente";
    private static final LocalDate NACIMIENTO = LocalDate.of(2000, 5, 1);

    @Autowired GoogleAuthService google;
    @Autowired RegistroService registro;
    @Autowired LoginService login;
    @Autowired UsuarioRepository usuarios;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean GoogleMobileTokenVerifier mobileTokens;

    @BeforeEach
    void prepararEstadoPendiente() {
        jdbc.update("INSERT INTO tipo_estado (nombre, descripcion) VALUES (?, ?)", "pendiente", "Pendiente");
    }

    @Test
    void nuevaCuentaEsperaFechaYConservaLaAprobacionHabitual() {
        assertEquals("completar", google.authenticate(solicitud("1200", null, null)).estado());
        assertFalse(usuarios.existsByCorreo(EMAIL));

        var cuenta = google.authenticate(solicitud("1200", NACIMIENTO, null)).usuario();
        assertEquals("CLIENTE", cuenta.rol());
        assertEquals("pendiente", cuenta.estado());
        assertEquals(NACIMIENTO, usuarios.findByCorreo(EMAIL).orElseThrow().getFechaNacimiento());
        assertEquals(cuenta, google.authenticate(solicitud("1200", null, null)).usuario());
        assertTrue(login.autenticar(new LoginRequest(EMAIL, PASSWORD)).isEmpty());
    }

    @Test
    void vinculacionConservaCuentaYContrasenaExistentes() {
        var cuenta = registro.registrar(new RegistroRequest("Ana", "Pérez", NACIMIENTO, EMAIL, PASSWORD));
        assertEquals("vincular", google.authenticate(solicitud("1200", null, null)).estado());

        var vinculada = google.authenticate(solicitud("1200", null, PASSWORD)).usuario();
        assertEquals(cuenta.idUsuario(), vinculada.idUsuario());
        assertEquals(vinculada, login.autenticar(new LoginRequest(EMAIL, PASSWORD)).orElseThrow());
        assertEquals(vinculada, google.authenticate(solicitud("1200", null, null)).usuario());
        assertEquals(1, usuarios.count());
        assertEquals(0, usuarios.vincularGoogle(cuenta.idUsuario(), EMAIL, "1250"));

        var error = assertThrows(ResponseStatusException.class,
                () -> google.authenticate(solicitud("1250", null, PASSWORD)));
        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void contrasenaErroneaDejaCuentaSinVincular() {
        registro.registrar(new RegistroRequest("Ana", "Pérez", NACIMIENTO, EMAIL, PASSWORD));

        var error = assertThrows(ResponseStatusException.class,
                () -> google.authenticate(solicitud("1200", null, "incorrecta")));
        assertEquals(401, error.getStatusCode().value());
        assertNull(usuarios.findByCorreo(EMAIL).orElseThrow().getGoogleSubject());
    }

    @Test
    void endpointExigeClavePrivadaYCorreoInstitucional() throws Exception {
        String datos = "{\"subject\":\"1200\",\"correo\":\"ana@tecsup.edu.pe\"}";
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content(datos)
                .header("X-Internal-Api-Key", "yourpeak-dev-internal-key"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                .content(datos.replace("tecsup.edu.pe", "gmail.com"))
                .header("X-Google-Internal-Key", "clave-interna-solo-pruebas-google"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content(datos)
                .header("X-Google-Internal-Key", "clave-interna-solo-pruebas-google"))
                .andExpect(status().isOk());
        assertFalse(usuarios.existsByCorreo(EMAIL));
    }

    @Test
    void registroMovilCreaCuentaCompartidaYConsumeLaSolicitud() throws Exception {
        String nonce = mobileChallenge();
        when(mobileTokens.verify("token", nonce)).thenReturn(new Identity("1300", EMAIL, "Ana", "Torres"));
        String token = """
                {"credential":"token","nonce":"%s"}
                """.formatted(nonce);
        String response = mvc.perform(post("/api/auth/google/mobile/login").contentType(MediaType.APPLICATION_JSON)
                .content(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("completar"))
                .andExpect(jsonPath("$.perfil.correo").value(EMAIL))
                .andReturn().getResponse().getContentAsString();
        assertFalse(usuarios.existsByCorreo(EMAIL));
        mvc.perform(post("/api/auth/google/mobile/login").contentType(MediaType.APPLICATION_JSON)
                .content(token)).andExpect(status().isGone());

        String flowId = JsonPath.read(response, "$.flowId");
        String completion = """
                {"flowId":"%s","nombre":"Ana","apellido":"Torres","fechaNacimiento":"2000-05-01"}
                """.formatted(flowId);
        mvc.perform(post("/api/auth/google/mobile/complete").contentType(MediaType.APPLICATION_JSON)
                .content(completion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.estado").value("pendiente"));
        mvc.perform(post("/api/auth/google/mobile/complete").contentType(MediaType.APPLICATION_JSON)
                .content(completion)).andExpect(status().isGone());
        assertEquals(usuarios.findByCorreo(EMAIL).orElseThrow().getIdUsuario(),
                google.authenticate(solicitud("1300", null, null)).usuario().idUsuario());
    }

    @Test
    void cuentaExistentePideVinculacionYConservaElLoginConContrasena() throws Exception {
        var cuenta = registro.registrar(new RegistroRequest("Ana", "Torres", NACIMIENTO, EMAIL, PASSWORD));
        String nonce = mobileChallenge();
        when(mobileTokens.verify("token", nonce)).thenReturn(new Identity("1400", EMAIL, "Ana", "Torres"));
        String response = mvc.perform(post("/api/auth/google/mobile/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"credential":"token","nonce":"%s"}
                        """.formatted(nonce)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("vincular"))
                .andReturn().getResponse().getContentAsString();
        String flowId = JsonPath.read(response, "$.flowId");
        mvc.perform(post("/api/auth/google/mobile/complete").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"flowId":"%s","contrasena":"%s"}
                        """.formatted(flowId, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.idUsuario").value(cuenta.idUsuario().intValue()));
        assertEquals(cuenta.idUsuario(), login.autenticar(new LoginRequest(EMAIL, PASSWORD)).orElseThrow().idUsuario());
        assertEquals(1, usuarios.count());
    }

    private String mobileChallenge() throws Exception {
        when(mobileTokens.clientId()).thenReturn("web-client");
        String response = mvc.perform(post("/api/auth/google/mobile/challenge"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.nonce");
    }

    private GoogleAuthRequest solicitud(String subject, LocalDate fecha, String contrasena) {
        return new GoogleAuthRequest(subject, EMAIL, "Ana", "Pérez", fecha, contrasena);
    }
}
