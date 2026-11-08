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

/** Un rango horario disponible de un cliente para un día de la semana. */
@Entity
@Table(
        name = "disponibilidad_cliente",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_disp_cliente_dia",
                columnNames = {"id_cliente", "dia_semana"}
        )
)
public class DisponibilidadCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_disponibilidad")
    private Long idDisponibilidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "dia_semana", nullable = false)
    private Byte diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    protected DisponibilidadCliente() {
        // Constructor requerido por JPA.
    }

    public DisponibilidadCliente(Cliente cliente, Integer diaSemana,
                                 LocalTime horaInicio, LocalTime horaFin) {
        this.cliente = cliente;
        this.diaSemana = diaSemana.byteValue();
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public Integer getDiaSemana() {
        return diaSemana.intValue();
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }
}
