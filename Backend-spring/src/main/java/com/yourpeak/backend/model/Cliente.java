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

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }
}