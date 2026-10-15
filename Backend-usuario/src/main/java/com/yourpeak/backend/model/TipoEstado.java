package com.yourpeak.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tipo_estado")
public class TipoEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_estado")
    private Integer idTipoEstado;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    @Column(name = "descripcion", length = 150)
    private String descripcion;

    protected TipoEstado() {
        // Constructor requerido por JPA.
    }

    public Integer getIdTipoEstado() {
        return idTipoEstado;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
}