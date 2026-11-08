package com.yourpeak.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Una versión semanal de la rutina de un cliente; las versiones anteriores se conservan inactivas. */
@Entity
@Table(name = "plan_entrenamiento")
public class PlanEntrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan_entrenamiento")
    private Long idPlanEntrenamiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_nivel_experiencia", nullable = false)
    private NivelExperiencia nivelExperiencia;

    @Column(name = "fecha_generacion", nullable = false, updatable = false)
    private LocalDateTime fechaGeneracion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_estado", nullable = false)
    private TipoEstado tipoEstado;

    protected PlanEntrenamiento() { }

    public PlanEntrenamiento(Cliente cliente, NivelExperiencia nivelExperiencia, TipoEstado tipoEstado) {
        this.cliente = cliente;
        this.nivelExperiencia = nivelExperiencia;
        this.tipoEstado = tipoEstado;
    }

    @PrePersist
    private void asignarFechaGeneracion() {
        if (fechaGeneracion == null) fechaGeneracion = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void cambiarEstado(TipoEstado estado) { this.tipoEstado = estado; }
    public Long getIdPlanEntrenamiento() { return idPlanEntrenamiento; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
}
