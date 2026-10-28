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

@Entity
@Table(name = "plan_membresia")
public class PlanMembresia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan_membresia")
    private Integer idPlanMembresia;

    @Column(name = "nombre", nullable = false, length = 60, unique = true)
    private String nombre;

    // La columna SQL es TINYINT con signo: admite valores entre 1 y 127.
    @Column(name = "duracion_meses", nullable = false)
    private Byte duracionMeses;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_estado", nullable = false)
    private TipoEstado tipoEstado;

    protected PlanMembresia() {
        // Constructor requerido por JPA.
    }

    public PlanMembresia(String nombre, Byte duracionMeses, TipoEstado tipoEstado) {
        this.nombre = nombre;
        this.duracionMeses = duracionMeses;
        this.tipoEstado = tipoEstado;
    }

    public void cambiarNombre(String nombre) {
        this.nombre = nombre;
    }

    public void cambiarEstado(TipoEstado tipoEstado) {
        this.tipoEstado = tipoEstado;
    }

    public Integer getIdPlanMembresia() {
        return idPlanMembresia;
    }

    public String getNombre() {
        return nombre;
    }

    public Byte getDuracionMeses() {
        return duracionMeses;
    }

    public TipoEstado getTipoEstado() {
        return tipoEstado;
    }
}
