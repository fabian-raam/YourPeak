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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa un período de membresía comprado por un cliente.
 * Cada renovación crea otra fila para conservar el historial.
 */
@Entity
@Table(name = "membresia")
public class Membresia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_membresia")
    private Long idMembresia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_administrador", nullable = false)
    private Administrador administrador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_plan_membresia", nullable = false)
    private PlanMembresia planMembresia;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    // La base de datos asigna la fecha de registro al crear cada período.
    @Column(name = "fecha_registro", nullable = false, updatable = false, insertable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "observacion", length = 500)
    private String observacion;

    protected Membresia() {
        // Constructor requerido por JPA.
    }

    public Membresia(
            Cliente cliente,
            Administrador administrador,
            PlanMembresia planMembresia,
            LocalDate fechaInicio,
            LocalDate fechaVencimiento,
            String observacion
    ) {
        this.cliente = cliente;
        this.administrador = administrador;
        this.planMembresia = planMembresia;
        this.fechaInicio = fechaInicio;
        this.fechaVencimiento = fechaVencimiento;
        this.observacion = observacion;
    }

    public Long getIdMembresia() {
        return idMembresia;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Administrador getAdministrador() {
        return administrador;
    }

    public PlanMembresia getPlanMembresia() {
        return planMembresia;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public String getObservacion() {
        return observacion;
    }
}
