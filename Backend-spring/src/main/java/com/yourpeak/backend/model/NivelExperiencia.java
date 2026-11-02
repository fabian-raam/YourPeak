package com.yourpeak.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "nivel_experiencia")
public class NivelExperiencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nivel_experiencia")
    private Integer idNivelExperiencia;

    @Column(name = "nombre", nullable = false, length = 30, unique = true)
    private String nombre;

    protected NivelExperiencia() { }

    public Integer getIdNivelExperiencia() { return idNivelExperiencia; }
    public String getNombre() { return nombre; }
}
