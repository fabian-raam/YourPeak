package com.yourpeak.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long idCliente;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_estado", nullable = false)
    private TipoEstado tipoEstado;

    @Column(name = "id_tipo_objetivo")
    private Integer idTipoObjetivo;

    @Column(name = "id_nivel_experiencia")
    private Integer idNivelExperiencia;

    @Column(name = "id_nivel_actividad")
    private Integer idNivelActividad;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    protected Cliente() {
        // Constructor requerido por JPA.
    }

    public Cliente(Usuario usuario, TipoEstado tipoEstado) {
        this.usuario = usuario;
        this.tipoEstado = tipoEstado;
    }

    @PrePersist
    private void asignarFechaAlta() {
        this.fechaAlta = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public TipoEstado getTipoEstado() {
        return tipoEstado;
    }

    /**
     * Activa al cliente después de que un administrador aprueba su registro.
     *
     * @param estadoActivo estado ACTIVO obtenido del catálogo tipo_estado
     * @throws IllegalStateException si el cliente no está PENDIENTE
     * @throws IllegalArgumentException si el estado recibido no es ACTIVO
     */
    public void activar(TipoEstado estadoActivo) {
        // La aprobación solo es válida para clientes que aún están pendientes.
        if (!"PENDIENTE".equalsIgnoreCase(tipoEstado.getNombre())) {
            throw new IllegalStateException(
                    "Solo se pueden activar clientes pendientes"
            );
        }

        // Recibimos el estado desde el catálogo; no creamos un estado nuevo aquí.
        if (estadoActivo == null || !"ACTIVO".equalsIgnoreCase(estadoActivo.getNombre())) {
            throw new IllegalArgumentException(
                    "El nuevo estado del cliente debe ser ACTIVO"
            );
        }

        // JPA detectará este cambio y lo guardará al confirmar la transacción.
        this.tipoEstado = estadoActivo;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }
}
