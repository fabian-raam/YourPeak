package com.yourpeak.backend;

import com.yourpeak.backend.dto.CambiarEstadoPlanRequest;
import com.yourpeak.backend.dto.CambiarNombrePlanRequest;
import com.yourpeak.backend.dto.PlanMembresiaRequest;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.Membresia;
import com.yourpeak.backend.model.PlanMembresia;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.model.Usuario;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.MembresiaRepository;
import com.yourpeak.backend.repository.PlanMembresiaRepository;
import com.yourpeak.backend.repository.TipoEstadoRepository;
import com.yourpeak.backend.repository.UsuarioRepository;
import com.yourpeak.backend.service.AprobacionService;
import com.yourpeak.backend.service.MembresiaService;
import com.yourpeak.backend.service.PlanMembresiaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pruebas de integración que verifican la persistencia real de membresías en H2. */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:planes-membresia;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class PlanMembresiaH2Tests {

    @Autowired
    private PlanMembresiaService servicio;

    @Autowired
    private PlanMembresiaRepository planes;

    @Autowired
    private AdministradorRepository administradores;

    @Autowired
    private MembresiaRepository membresias;

    @Autowired
    private ClienteRepository clientes;

    @Autowired
    private TipoEstadoRepository estados;

    @Autowired
    private MembresiaService servicioMembresias;

    @Autowired
    private AprobacionService servicioAprobacion;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private JdbcTemplate jdbc;

    private Long idUsuarioAdministrador;

    @BeforeEach
    void prepararDatos() {
        // Los estados son datos de catálogo que la lógica de planes consulta en la BD.
        jdbc.update("INSERT INTO tipo_estado (nombre, descripcion) VALUES (?, ?)", "PENDIENTE", "Pendiente de aprobación");
        jdbc.update("INSERT INTO tipo_estado (nombre, descripcion) VALUES (?, ?)", "ACTIVO", "Estado activo");
        jdbc.update("INSERT INTO tipo_estado (nombre, descripcion) VALUES (?, ?)", "INACTIVO", "Estado inactivo");

        Usuario administrador = usuarios.saveAndFlush(new Usuario(
                "Admin", "Prueba", "admin-planes@tecsup.edu.pe", "hash-de-prueba",
                LocalDate.of(1990, 1, 1)
        ));
        idUsuarioAdministrador = administrador.getIdUsuario();

        // Se inserta la relación que identifica a este usuario como administrador.
        jdbc.update("INSERT INTO administrador (id_usuario) VALUES (?)", idUsuarioAdministrador);
    }

    @Test
    void crearGuardaElPlanEnLaBaseDeDatos() {
        var respuesta = servicio.crear(new PlanMembresiaRequest("Plan mensual", 1), idUsuarioAdministrador);

        var guardado = planes.findById(respuesta.idPlanMembresia()).orElseThrow();
        assertEquals("Plan mensual", guardado.getNombre());
        assertEquals(1, guardado.getDuracionMeses().intValue());
        assertEquals("ACTIVO", guardado.getTipoEstado().getNombre());
    }

    @Test
    void listarDevuelveLosPlanesGuardadosEnLaBaseDeDatos() {
        servicio.crear(new PlanMembresiaRequest("Plan mensual", 1), idUsuarioAdministrador);
        servicio.crear(new PlanMembresiaRequest("Plan trimestral", 3), idUsuarioAdministrador);

        var resultado = servicio.listar(idUsuarioAdministrador);

        assertEquals(2, resultado.size());
        assertEquals("Plan mensual", resultado.get(0).nombre());
        assertEquals("Plan trimestral", resultado.get(1).nombre());
    }

    @Test
    void consultarPorIdDevuelveElPlanPersistido() {
        var creado = servicio.crear(new PlanMembresiaRequest("Plan mensual", 1), idUsuarioAdministrador);

        var resultado = servicio.obtener(creado.idPlanMembresia(), idUsuarioAdministrador);

        assertEquals(creado.idPlanMembresia(), resultado.idPlanMembresia());
        assertEquals("Plan mensual", resultado.nombre());
    }

    @Test
    void cambiarNombrePersisteElNombreSinModificarDuracionNiEstado() {
        var creado = servicio.crear(new PlanMembresiaRequest("Plan mensual", 1), idUsuarioAdministrador);

        servicio.actualizar(creado.idPlanMembresia(), new CambiarNombrePlanRequest("Mensual renovado"), idUsuarioAdministrador);

        var guardado = planes.findById(creado.idPlanMembresia()).orElseThrow();
        assertEquals("Mensual renovado", guardado.getNombre());
        assertEquals(1, guardado.getDuracionMeses().intValue());
        assertEquals("ACTIVO", guardado.getTipoEstado().getNombre());
    }

    @Test
    void cambiarEstadoPersisteLaDesactivacionYReactivacion() {
        var creado = servicio.crear(new PlanMembresiaRequest("Plan mensual", 1), idUsuarioAdministrador);

        servicio.cambiarEstado(creado.idPlanMembresia(), new CambiarEstadoPlanRequest("INACTIVO"), idUsuarioAdministrador);
        assertEquals("INACTIVO", planes.findById(creado.idPlanMembresia()).orElseThrow()
                .getTipoEstado().getNombre());

        servicio.cambiarEstado(creado.idPlanMembresia(), new CambiarEstadoPlanRequest("ACTIVO"), idUsuarioAdministrador);
        assertEquals("ACTIVO", planes.findById(creado.idPlanMembresia()).orElseThrow()
                .getTipoEstado().getNombre());
    }

    @Test
    void aprobarClienteGuardaSuPeriodoEnLaMismaOperacion() {
        Cliente cliente = crearCliente("pendiente@tecsup.edu.pe", "PENDIENTE");
        var plan = servicio.crear(new PlanMembresiaRequest("Plan de aprobación", 1), idUsuarioAdministrador);

        Membresia periodo = servicioAprobacion.aprobarCliente(
                cliente.getIdCliente(),
                plan.idPlanMembresia(),
                null,
                idUsuarioAdministrador
        );

        assertEquals("ACTIVO", clientes.findById(cliente.getIdCliente()).orElseThrow()
                .getTipoEstado().getNombre());
        Membresia guardada = membresias.findById(periodo.getIdMembresia()).orElseThrow();
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        assertEquals(hoy, guardada.getFechaInicio());
        assertEquals(hoy.plusMonths(1).minusDays(1), guardada.getFechaVencimiento());
        assertEquals("Plan de aprobación", guardada.getPlanMembresia().getNombre());
    }

    @Test
    void renovarAntesDelVencimientoProgramaElNuevoPeriodoAlDiaSiguiente() {
        Cliente cliente = crearCliente("renovacion@tecsup.edu.pe", "ACTIVO");
        var plan = servicio.crear(new PlanMembresiaRequest("Plan renovación", 3), idUsuarioAdministrador);
        var entidadPlan = planes.findById(plan.idPlanMembresia()).orElseThrow();
        var administrador = administradores.findByUsuario_IdUsuario(idUsuarioAdministrador).orElseThrow();
        LocalDate vencimientoActual = LocalDate.now(ZoneId.of("America/Lima")).plusDays(12);
        membresias.saveAndFlush(new Membresia(
                cliente,
                administrador,
                entidadPlan,
                vencimientoActual.minusMonths(1),
                vencimientoActual,
                null
        ));

        Membresia renovacion = servicioMembresias.registrarPeriodo(
                cliente.getIdCliente(),
                plan.idPlanMembresia(),
                idUsuarioAdministrador,
                null
        );

        assertEquals(vencimientoActual.plusDays(1), renovacion.getFechaInicio());
        assertEquals(vencimientoActual.plusDays(1).plusMonths(3).minusDays(1),
                renovacion.getFechaVencimiento());
        assertEquals(2L, membresias.count());
    }

    @Test
    void renovarDespuesDelVencimientoEmpiezaHoy() {
        Cliente cliente = crearCliente("vencida@tecsup.edu.pe", "ACTIVO");
        var plan = servicio.crear(new PlanMembresiaRequest("Plan posterior", 1), idUsuarioAdministrador);
        var entidadPlan = planes.findById(plan.idPlanMembresia()).orElseThrow();
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        LocalDate vencimientoAnterior = hoy.minusDays(1);
        Membresia anterior = new Membresia(
                cliente,
                administradores.findByUsuario_IdUsuario(idUsuarioAdministrador).orElseThrow(),
                entidadPlan,
                vencimientoAnterior.minusMonths(1),
                vencimientoAnterior,
                null
        );
        membresias.saveAndFlush(anterior);

        Membresia renovacion = servicioMembresias.registrarPeriodo(
                cliente.getIdCliente(),
                plan.idPlanMembresia(),
                idUsuarioAdministrador,
                null
        );

        assertEquals(hoy, renovacion.getFechaInicio());
    }

    @Test
    void listarClientesActivosIncluyeElEstadoDeSuMembresia() {
        Cliente cliente = crearCliente("cliente-activo@tecsup.edu.pe", "ACTIVO");
        var plan = servicio.crear(new PlanMembresiaRequest("Plan activo", 1), idUsuarioAdministrador);
        var entidadPlan = planes.findById(plan.idPlanMembresia()).orElseThrow();
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        membresias.saveAndFlush(new Membresia(
                cliente,
                administradores.findByUsuario_IdUsuario(idUsuarioAdministrador).orElseThrow(),
                entidadPlan,
                hoy,
                hoy.plusMonths(1).minusDays(1),
                null
        ));

        var resultado = servicioMembresias.listarClientesActivos(idUsuarioAdministrador);

        assertEquals(1, resultado.size());
        assertEquals(cliente.getIdCliente(), resultado.get(0).idCliente());
        assertEquals("VIGENTE", resultado.get(0).estadoMembresia());
        assertEquals(hoy.plusMonths(1).minusDays(1), resultado.get(0).fechaVencimiento());
    }

    private Cliente crearCliente(String correo, String estadoNombre) {
        Usuario usuario = usuarios.saveAndFlush(new Usuario(
                "Cliente", "Prueba", correo, "hash-de-prueba", LocalDate.of(1995, 5, 5)
        ));
        TipoEstado estado = estados.findByNombre(estadoNombre).orElseThrow();
        return clientes.saveAndFlush(new Cliente(usuario, estado));
    }

}
