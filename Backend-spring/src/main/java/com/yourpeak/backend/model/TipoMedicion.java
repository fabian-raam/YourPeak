package com.yourpeak.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_medicion")
public class TipoMedicion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_medicion")
    private Short idTipoMedicion;

    @Column(name = "nombre", nullable = false, length = 30, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 150)
    private String descripcion;

    protected TipoMedicion() { }

    public Short getIdTipoMedicion() { return idTipoMedicion; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
}
