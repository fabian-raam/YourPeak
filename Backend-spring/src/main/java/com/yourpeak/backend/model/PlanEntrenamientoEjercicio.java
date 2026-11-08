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

/** Configuración de un ejercicio dentro de un día de rutina. */
@Entity
@Table(name = "plan_entrenamiento_ejercicio")
public class PlanEntrenamientoEjercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan_ejercicio")
    private Long idPlanEjercicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_plan_dia", nullable = false)
    private PlanEntrenamientoDia planDia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ejercicio", nullable = false)
    private Ejercicio ejercicio;

    @Column(name = "orden", nullable = false)
    private Byte orden;

    @Column(name = "series", nullable = false)
    private Byte series;

    @Column(name = "repeticiones")
    private Byte repeticiones;

    @Column(name = "segundos")
    private Short segundos;

    @Column(name = "descanso_seg", nullable = false)
    private Short descansoSegundos;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_origen_ejercicio", nullable = false)
    private TipoOrigenEjercicio tipoOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_estado", nullable = false)
    private TipoEstado tipoEstado;

    protected PlanEntrenamientoEjercicio() { }

    public PlanEntrenamientoEjercicio(PlanEntrenamientoDia planDia, Ejercicio ejercicio, Integer orden,
                                      Integer series, Integer repeticiones, Integer segundos,
                                      Integer descansoSegundos, TipoOrigenEjercicio origen,
                                      TipoEstado estado) {
        this.planDia = planDia;
        this.ejercicio = ejercicio;
        this.orden = orden.byteValue();
        this.series = series.byteValue();
        this.repeticiones = repeticiones == null ? null : repeticiones.byteValue();
        this.segundos = segundos == null ? null : segundos.shortValue();
        this.descansoSegundos = descansoSegundos.shortValue();
        this.tipoOrigen = origen;
        this.tipoEstado = estado;
    }

    public Long getIdPlanEjercicio() { return idPlanEjercicio; }
    public Ejercicio getEjercicio() { return ejercicio; }
    public Integer getOrden() { return orden.intValue(); }
    public Integer getSeries() { return series.intValue(); }
    public Integer getRepeticiones() { return repeticiones == null ? null : repeticiones.intValue(); }
    public Integer getSegundos() { return segundos == null ? null : segundos.intValue(); }
    public Integer getDescansoSegundos() { return descansoSegundos.intValue(); }
    public TipoOrigenEjercicio getTipoOrigen() { return tipoOrigen; }
}
