package com.yourpeak.backend;

import com.yourpeak.backend.dto.RegistroRequest;
import com.yourpeak.backend.model.Usuario;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.UsuarioRepository;
import com.yourpeak.backend.service.AprobacionService;
import com.yourpeak.backend.service.LoginService;
import com.yourpeak.backend.service.PanelAdministradorService;
import com.yourpeak.backend.service.RegistroService;
import com.yourpeak.backend.service.SolicitudAccesoService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Flujo completo: registro, rechazo con motivo, nueva solicitud y tope diario. */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:solicitudes;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class SolicitudAccesoH2Tests {

    @Autowired private RegistroService registro;
    @Autowired private AprobacionService aprobacion;
    @Autowired private SolicitudAccesoService solicitudes;
    @Autowired private LoginService login;
    @Autowired private PanelAdministradorService panel;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ClienteRepository clientes;
    @Autowired private JdbcTemplate jdbc;

    private Long idAdmin;
    private Long idUsuario;
    private Long idCliente;

    @BeforeEach
    void preparar() {
        for (String estado : new String[] {"pendiente", "ACTIVO", "INACTIVO", "RECHAZADO"}) {
            jdbc.update("INSERT INTO tipo_estado (nombre, descripcion) VALUES (?, ?)", estado, estado);
        }
        idAdmin = usuarios.saveAndFlush(new Usuario("Admin", "Prueba", "admin-sol@tecsup.edu.pe", "hash",
                LocalDate.of(1990, 1, 1))).getIdUsuario();
        jdbc.update("INSERT INTO administrador (id_usuario) VALUES (?)", idAdmin);

        var creado = registro.registrar(new RegistroRequest("Mateo", "Ríos", LocalDate.of(2003, 4, 12),
                "mateo.rios@tecsup.edu.pe", "frase segura 1"));
        idUsuario = creado.idUsuario();
        idCliente = creado.idCliente();
    }

    @Test
    void elRegistroCreaLaPrimeraSolicitudPendiente() {
        var estado = solicitudes.consultar(idUsuario);

        assertEquals("PENDIENTE", estado.estado());
        assertEquals(1, estado.historial().size());
        assertFalse(estado.puedeSolicitar());
        assertEquals(1, aprobacion.listarPendientes(idAdmin).get(0).numeroSolicitud());
    }

    @Test
    void unRechazoMuestraElMotivoYPermiteVolverASolicitar() {
        aprobacion.rechazarCliente(idCliente, "No se registró el pago.", idAdmin);

        var rechazado = solicitudes.consultar(idUsuario);
        assertEquals("RECHAZADO", rechazado.estado());
        assertEquals("No se registró el pago.", rechazado.historial().get(0).motivoRechazo());
        assertTrue(rechazado.puedeSolicitar());
        assertEquals("RECHAZADO", login.buscarUsuario(idUsuario).orElseThrow().estado());

        var reintento = solicitudes.volverASolicitar(idUsuario, "Ya pagué en recepción.");
        assertEquals("PENDIENTE", reintento.estado());
        assertEquals(2, reintento.historial().size());

        var pendiente = aprobacion.listarPendientes(idAdmin).get(0);
        assertEquals(2, pendiente.numeroSolicitud());
        assertEquals("Ya pagué en recepción.", pendiente.mensajeCliente());
        assertEquals("No se registró el pago.", pendiente.motivoRechazoAnterior());
    }

    @Test
    void noSePuedeVolverASolicitarMientrasSigaPendiente() {
        var error = assertThrows(ResponseStatusException.class, () -> solicitudes.volverASolicitar(idUsuario, null));
        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void elTopeDiarioImpideMasSolicitudes() {
        // Registro + 2 reintentos = 3 solicitudes hoy, el máximo permitido.
        for (int i = 0; i < SolicitudAccesoService.LIMITE_DIARIO - 1; i++) {
            aprobacion.rechazarCliente(idCliente, "Datos incompletos.", idAdmin);
            solicitudes.volverASolicitar(idUsuario, null);
        }
        aprobacion.rechazarCliente(idCliente, "Datos incompletos.", idAdmin);

        var estado = solicitudes.consultar(idUsuario);
        assertFalse(estado.puedeSolicitar());
        assertEquals(0, estado.solicitudesRestantesHoy());
        var error = assertThrows(ResponseStatusException.class, () -> solicitudes.volverASolicitar(idUsuario, null));
        assertEquals(429, error.getStatusCode().value());
        assertEquals("RECHAZADO", clientes.findById(idCliente).orElseThrow().getTipoEstado().getNombre());
    }

    @Test
    void laActividadDevuelveUnValorPorDia() {
        var actividad = panel.consultarActividad(idAdmin, 14);

        assertEquals(14, actividad.size());
        assertEquals(LocalDate.now(java.time.ZoneId.of("America/Lima")), actividad.get(13).fecha());
        assertTrue(actividad.stream().allMatch(dia -> dia.sesiones() == 0));
    }
}
