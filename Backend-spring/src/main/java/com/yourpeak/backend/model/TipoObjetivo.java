package com.yourpeak.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_objetivo")
public class TipoObjetivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_objetivo")
    private Integer idTipoObjetivo;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 150)
    private String descripcion;

    protected TipoObjetivo() { }

    public Integer getIdTipoObjetivo() { return idTipoObjetivo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
}
