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
import java.time.ZoneId;

/** Registra una serie que el cliente completó en un ejercicio de su rutina. */
@Entity
@Table(name = "serie_realizada")
public class SerieRealizada {
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_serie_realizada")
    private Long idSerieRealizada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_plan_ejercicio", nullable = false)
    private PlanEntrenamientoEjercicio planEjercicio;

    @Column(name = "numero_serie", nullable = false)
    private Byte numeroSerie;

    @Column(name = "repeticiones")
    private Byte repeticiones;

    @Column(name = "segundos")
    private Short segundos;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    protected SerieRealizada() { }

    public SerieRealizada(PlanEntrenamientoEjercicio planEjercicio, Integer numeroSerie,
                         Integer repeticiones, Integer segundos) {
        this.planEjercicio = planEjercicio;
        this.numeroSerie = numeroSerie.byteValue();
        this.repeticiones = repeticiones == null ? null : repeticiones.byteValue();
        this.segundos = segundos == null ? null : segundos.shortValue();
    }

    @PrePersist
    private void asignarFechaHora() {
        if (fechaHora == null) fechaHora = LocalDateTime.now(ZONA_LIMA);
    }
}
