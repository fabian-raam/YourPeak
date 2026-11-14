package com.yourpeak.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "nivel_actividad")
public class NivelActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nivel_actividad")
    private Integer idNivelActividad;

    @Column(name = "nombre", nullable = false, length = 30, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 150)
    private String descripcion;

    protected NivelActividad() { }

    public Integer getIdNivelActividad() { return idNivelActividad; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
}
