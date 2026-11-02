package com.yourpeak.backend;

import com.yourpeak.backend.dto.CambiarEstadoEjercicioRequest;
import com.yourpeak.backend.dto.EjercicioRequest;
import com.yourpeak.backend.repository.EjercicioRepository;
import com.yourpeak.backend.service.EjercicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Comprueba el CRUD lógico del catálogo usando persistencia H2 y las entidades JPA. */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogo-ejercicios;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class EjercicioCatalogoH2Tests {
    private static final Long ID_ADMIN = 1L;

    @Autowired private EjercicioService servicio;
    @Autowired private EjercicioRepository ejercicios;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void prepararCatalogos() {
        jdbc.update("INSERT INTO tipo_estado (id_tipo_estado, nombre, descripcion) VALUES (1, 'PENDIENTE', 'Pendiente')");
        jdbc.update("INSERT INTO tipo_estado (id_tipo_estado, nombre, descripcion) VALUES (2, 'ACTIVO', 'Activo')");
        jdbc.update("INSERT INTO tipo_estado (id_tipo_estado, nombre, descripcion) VALUES (3, 'INACTIVO', 'Inactivo')");
        jdbc.update("INSERT INTO nivel_experiencia (id_nivel_experiencia, nombre) VALUES (1, 'Principiante')");
        jdbc.update("INSERT INTO nivel_experiencia (id_nivel_experiencia, nombre) VALUES (2, 'Intermedio')");
        jdbc.update("INSERT INTO grupo_muscular (id_grupo_muscular, nombre) VALUES (1, 'Pecho')");
        jdbc.update("INSERT INTO grupo_muscular (id_grupo_muscular, nombre) VALUES (2, 'Espalda')");
        jdbc.update("INSERT INTO tipo_medicion (id_tipo_medicion, nombre, descripcion) VALUES (1, 'REPETICIONES', 'Por cantidad')");
        jdbc.update("INSERT INTO tipo_medicion (id_tipo_medicion, nombre, descripcion) VALUES (2, 'TIEMPO', 'Por segundos')");
        jdbc.update("INSERT INTO usuario (id_usuario, nombre, apellido, correo, contrasena_hash, fecha_nacimiento, fecha_creacion) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                ID_ADMIN, "Admin", "Prueba", "admin-ejercicios@tecsup.edu.pe", "hash", LocalDate.of(1990, 1, 1));
        jdbc.update("INSERT INTO administrador (id_usuario) VALUES (?)", ID_ADMIN);
    }

    @Test
    void crearPersisteEjercicioActivoConSusCatalogos() {
        var creado = servicio.crear(request("Press banca", 1, 1, (short) 1), ID_ADMIN);

        var guardado = ejercicios.findById(creado.idEjercicio()).orElseThrow();
        assertEquals("Press banca", guardado.getNombre());
        assertEquals("Pecho", guardado.getGrupoMuscular().getNombre());
        assertEquals("Principiante", guardado.getNivelExperiencia().getNombre());
        assertEquals("REPETICIONES", guardado.getTipoMedicion().getNombre());
        assertEquals("ACTIVO", guardado.getTipoEstado().getNombre());
    }

    @Test
    void actualizarEditaLosDatosSinCambiarEstado() {
        var creado = servicio.crear(request("Press banca", 1, 1, (short) 1), ID_ADMIN);

        var actualizado = servicio.actualizar(
                creado.idEjercicio(), request("Press inclinado", 2, 2, (short) 2), ID_ADMIN);

        assertEquals("Press inclinado", actualizado.nombre());
        assertEquals("Espalda", actualizado.grupoMuscular());
        assertEquals("TIEMPO", actualizado.tipoMedicion());
        assertEquals("ACTIVO", actualizado.estado());
    }

    @Test
    void cambiarEstadoDesactivaYReactivaSinEliminarElEjercicio() {
        var creado = servicio.crear(request("Press banca", 1, 1, (short) 1), ID_ADMIN);

        servicio.cambiarEstado(creado.idEjercicio(), new CambiarEstadoEjercicioRequest("INACTIVO"), ID_ADMIN);
        assertEquals("INACTIVO", ejercicios.findById(creado.idEjercicio()).orElseThrow()
                .getTipoEstado().getNombre());

        servicio.cambiarEstado(creado.idEjercicio(), new CambiarEstadoEjercicioRequest("ACTIVO"), ID_ADMIN);
        assertEquals(1, ejercicios.count());
        assertEquals("ACTIVO", ejercicios.findById(creado.idEjercicio()).orElseThrow()
                .getTipoEstado().getNombre());
    }

    @Test
    void rechazaUnNombreDuplicado() {
        servicio.crear(request("Press banca", 1, 1, (short) 1), ID_ADMIN);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(request("press BANCA", 1, 1, (short) 1), ID_ADMIN));
        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void rechazaCatalogoInexistenteYUsuarioNoAdministrador() {
        ResponseStatusException grupoInvalido = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(request("Press banca", 999, 1, (short) 1), ID_ADMIN));
        assertEquals(400, grupoInvalido.getStatusCode().value());

        ResponseStatusException noAdmin = assertThrows(ResponseStatusException.class,
                () -> servicio.listar(999L));
        assertEquals(403, noAdmin.getStatusCode().value());
    }

    private EjercicioRequest request(String nombre, int grupo, int nivel, short medicion) {
        return new EjercicioRequest(grupo, nivel, medicion, nombre,
                "Descripción de prueba", "Barra", null);
    }
}
