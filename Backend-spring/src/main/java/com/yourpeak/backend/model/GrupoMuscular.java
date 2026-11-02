package com.yourpeak.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "grupo_muscular")
public class GrupoMuscular {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grupo_muscular")
    private Integer idGrupoMuscular;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;

    protected GrupoMuscular() { }

    public Integer getIdGrupoMuscular() { return idGrupoMuscular; }
    public String getNombre() { return nombre; }
}
