package com.yourpeak.backend;

import com.yourpeak.backend.dto.AlimentoRequest;
import com.yourpeak.backend.service.AlimentoService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogo-alimentos;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class AlimentoH2Tests {
    private static final Long ID_ADMIN = 1L;

    @Autowired private AlimentoService servicio;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Validator validator;

    @BeforeEach
    void prepararAdministrador() {
        jdbc.update("INSERT INTO usuario (id_usuario, nombre, apellido, correo, contrasena_hash, fecha_nacimiento, fecha_creacion) "
                        + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                ID_ADMIN, "Admin", "Prueba", "admin-alimentos@tecsup.edu.pe", "hash", LocalDate.of(1990, 1, 1));
        jdbc.update("INSERT INTO administrador (id_usuario) VALUES (?)", ID_ADMIN);
    }

    @Test
    void crearGuardaPorcionYValoresNutricionales() {
        var creado = servicio.crear(request("Avena", "40.00", "150.00", "5.00", "27.00", "3.00"), ID_ADMIN);

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM alimento", Integer.class));
        assertEquals(new BigDecimal("40.00"), jdbc.queryForObject(
                "SELECT porcion_base_g FROM alimento WHERE id_alimento = ?", BigDecimal.class, creado.idAlimento()));
        assertEquals(new BigDecimal("150.00"), creado.kcal());
        assertEquals(new BigDecimal("5.00"), creado.proteinaG());
        assertEquals(new BigDecimal("27.00"), creado.carbohidratoG());
        assertEquals(new BigDecimal("3.00"), creado.grasaG());
    }

    @Test
    void actualizarPersisteElNombreLaPorcionYLosMacros() {
        var creado = servicio.crear(request("Avena", "40.00", "150.00", "5.00", "27.00", "3.00"), ID_ADMIN);

        var actualizado = servicio.actualizar(creado.idAlimento(),
                request("Avena integral", "50.00", "190.00", "6.50", "32.00", "3.50"), ID_ADMIN);

        assertEquals("Avena integral", actualizado.nombre());
        assertEquals(new BigDecimal("50.00"), actualizado.porcionBaseG());
        assertEquals(new BigDecimal("190.00"), actualizado.kcal());
        assertEquals(new BigDecimal("6.50"), actualizado.proteinaG());
        assertEquals(new BigDecimal("32.00"), actualizado.carbohidratoG());
        assertEquals(new BigDecimal("3.50"), actualizado.grasaG());
    }

    @Test
    void rechazaNombreDuplicadoIgnorandoMayusculas() {
        servicio.crear(request("Avena", "40.00", "150.00", "5.00", "27.00", "3.00"), ID_ADMIN);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(request("AVENA", "40.00", "150.00", "5.00", "27.00", "3.00"), ID_ADMIN));

        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void rechazaAlimentoInexistente() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.obtener(999, ID_ADMIN));

        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void soloAdministradorPuedeConsultarYModificarElCatalogo() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.listar(999L));

        assertEquals(403, error.getStatusCode().value());
    }

    @Test
    void validacionRechazaPorcionCeroYValoresNegativos() {
        var invalido = new AlimentoRequest("Avena", BigDecimal.ZERO, BigDecimal.valueOf(-1),
                BigDecimal.valueOf(-2), BigDecimal.ZERO, BigDecimal.ZERO);

        var camposInvalidos = validator.validate(invalido).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .toList();

        assertFalse(camposInvalidos.isEmpty());
        assertEquals(true, camposInvalidos.contains("porcionBaseG"));
        assertEquals(true, camposInvalidos.contains("kcal"));
        assertEquals(true, camposInvalidos.contains("proteinaG"));
    }

    @Test
    void validacionRechazaCamposObligatoriosVacios() {
        var invalido = new AlimentoRequest(" ", null, null, null, null, null);

        assertFalse(validator.validate(invalido).isEmpty());
    }

    private AlimentoRequest request(String nombre, String porcion, String kcal,
                                    String proteina, String carbohidrato, String grasa) {
        return new AlimentoRequest(nombre, new BigDecimal(porcion), new BigDecimal(kcal),
                new BigDecimal(proteina), new BigDecimal(carbohidrato), new BigDecimal(grasa));
    }
}
