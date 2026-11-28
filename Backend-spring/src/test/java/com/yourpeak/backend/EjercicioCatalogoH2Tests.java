package com.yourpeak.backend;

import com.yourpeak.backend.dto.CambiarEstadoEjercicioRequest;
import com.yourpeak.backend.dto.EjercicioRequest;
import com.yourpeak.backend.dto.EjercicioResponse;
import com.yourpeak.backend.repository.EjercicioRepository;
import com.yourpeak.backend.service.EjercicioService;
import com.yourpeak.backend.service.ia.ClienteGemini;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Comprueba el CRUD lógico del catálogo usando persistencia H2 y las entidades JPA. */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogo-ejercicios;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "yourpeak.internal-api-key=pruebas-imagenes"
})
@Transactional
class EjercicioCatalogoH2Tests {
    private static final Long ID_ADMIN = 1L;

    @Autowired private EjercicioService servicio;
    @Autowired private EjercicioRepository ejercicios;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private WebApplicationContext web;
    @MockitoBean private ClienteGemini gemini;
    @TempDir static Path imagenes;

    @DynamicPropertySource
    static void configurarImagenes(DynamicPropertyRegistry registry) {
        registry.add("yourpeak.ejercicios.imagenes-dir", () -> imagenes.toString());
    }

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

    @Test
    void creaConsultaConservaYReemplazaImagen() throws Exception {
        byte[] primeraImagen = png(0xff445566);
        var mvc = MockMvcBuilders.webAppContextSetup(web)
                .addFilters(web.getBean("springSecurityFilterChain", Filter.class))
                .build();
        var json = JsonMapper.builder().build();
        var carga = post("/api/admin/ejercicios")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Admin-User-Id", ID_ADMIN)
                .content(json.writeValueAsBytes(requestConImagen("Press banca", primeraImagen)));
        mvc.perform(carga).andExpect(status().isForbidden());
        var resultado = mvc.perform(carga.header("X-Internal-Api-Key", "pruebas-imagenes"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsByteArray();
        var creado = json.readValue(resultado, EjercicioResponse.class);
        mvc.perform(get(creado.imagenRuta()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(primeraImagen));

        var editado = servicio.actualizar(creado.idEjercicio(),
                request("Press inclinado", 1, 1, (short) 1), ID_ADMIN);
        assertEquals(creado.imagenRuta(), editado.imagenRuta());
        byte[] segundaImagen = png(0xffaabbcc);
        var reemplazado = servicio.actualizar(creado.idEjercicio(),
                requestConImagen("Press inclinado", segundaImagen), ID_ADMIN);
        assertNotEquals(creado.imagenRuta(), reemplazado.imagenRuta());
        assertEquals(reemplazado.imagenRuta(), servicio.obtener(creado.idEjercicio(), ID_ADMIN).imagenRuta());
        mvc.perform(get(reemplazado.imagenRuta()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(segundaImagen));
    }

    @Test
    void descartaArchivoInvalidoYConservaEjercicio() {
        var creado = servicio.crear(request("Press banca", 1, 1, (short) 1), ID_ADMIN);
        var error = assertThrows(ResponseStatusException.class, () -> servicio.actualizar(
                creado.idEjercicio(), requestConImagen("Press inclinado", new byte[]{1, 2, 3}), ID_ADMIN));
        assertEquals(400, error.getStatusCode().value());
        var guardado = servicio.obtener(creado.idEjercicio(), ID_ADMIN);
        assertEquals("Press banca", guardado.nombre());
        assertNull(guardado.imagenRuta());
    }

    private static byte[] png(int color) throws IOException {
        var imagen = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        imagen.setRGB(0, 0, color);
        var salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private EjercicioRequest requestConImagen(String nombre, byte[] imagen) {
        return new EjercicioRequest(1, 1, (short) 1, nombre,
                null, null, null, Base64.getEncoder().encodeToString(imagen));
    }

    private EjercicioRequest request(String nombre, int grupo, int nivel, short medicion) {
        return new EjercicioRequest(grupo, nivel, medicion, nombre,
                "Descripción de prueba", "Barra", null);
    }
}
