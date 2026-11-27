package com.yourpeak.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Cada vez que un cliente pide acceso queda una solicitud que el administrador aprueba o rechaza. */
@Entity
@Table(name = "solicitud_acceso")
public class SolicitudAcceso {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String APROBADA = "APROBADA";
    public static final String RECHAZADA = "RECHAZADA";
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud_acceso")
    private Long idSolicitudAcceso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "estado", nullable = false, length = 15)
    private String estado;

    @Column(name = "mensaje_cliente", length = 300)
    private String mensajeCliente;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "id_administrador")
    private Long idAdministrador;

    protected SolicitudAcceso() {
        // Constructor requerido por JPA.
    }

    public SolicitudAcceso(Cliente cliente, String mensajeCliente) {
        this.cliente = cliente;
        this.estado = PENDIENTE;
        this.mensajeCliente = mensajeCliente;
        this.fechaSolicitud = LocalDateTime.now(ZONA_LIMA);
    }

    public void aprobar(Long idAdministrador) {
        resolver(APROBADA, null, idAdministrador);
    }

    public void rechazar(String motivo, Long idAdministrador) {
        resolver(RECHAZADA, motivo, idAdministrador);
    }

    private void resolver(String nuevoEstado, String motivo, Long idAdministrador) {
        if (!PENDIENTE.equals(estado)) {
            throw new IllegalStateException("La solicitud ya fue resuelta");
        }
        this.estado = nuevoEstado;
        this.motivoRechazo = motivo;
        this.idAdministrador = idAdministrador;
        this.fechaResolucion = LocalDateTime.now(ZONA_LIMA);
    }

    public Long getIdSolicitudAcceso() { return idSolicitudAcceso; }
    public Cliente getCliente() { return cliente; }
    public String getEstado() { return estado; }
    public String getMensajeCliente() { return mensajeCliente; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
}
