package com.yourpeak.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catálogo que indica si el ejercicio de la rutina lo agregó el usuario o un generador. */
@Entity
@Table(name = "tipo_origen_ejercicio")
public class TipoOrigenEjercicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_origen_ejercicio")
    private Short idTipoOrigenEjercicio;

    @Column(name = "nombre", nullable = false, length = 30, unique = true)
    private String nombre;

    protected TipoOrigenEjercicio() { }
    public String getNombre() { return nombre; }
}
