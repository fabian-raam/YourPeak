package com.yourpeak.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalTime;

/** Día de entrenamiento dentro de una versión semanal de rutina. */
@Entity
@Table(name = "plan_entrenamiento_dia", uniqueConstraints = @UniqueConstraint(
        name = "uq_plandia", columnNames = {"id_plan_entrenamiento", "dia_semana"}
))
public class PlanEntrenamientoDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan_dia")
    private Long idPlanDia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_plan_entrenamiento", nullable = false)
    private PlanEntrenamiento planEntrenamiento;

    @Column(name = "dia_semana", nullable = false)
    private Byte diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    protected PlanEntrenamientoDia() { }

    public PlanEntrenamientoDia(PlanEntrenamiento plan, Integer dia, LocalTime inicio, LocalTime fin) {
        this.planEntrenamiento = plan;
        this.diaSemana = dia.byteValue();
        this.horaInicio = inicio;
        this.horaFin = fin;
    }

    public Long getIdPlanDia() { return idPlanDia; }
    public Integer getDiaSemana() { return diaSemana.intValue(); }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
}
