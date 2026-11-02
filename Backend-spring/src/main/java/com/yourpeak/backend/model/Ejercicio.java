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
@Table(name = "ejercicio")
public class Ejercicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ejercicio")
    private Integer idEjercicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_grupo_muscular", nullable = false)
    private GrupoMuscular grupoMuscular;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_nivel_experiencia", nullable = false)
    private NivelExperiencia nivelExperiencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_medicion", nullable = false)
    private TipoMedicion tipoMedicion;

    @Column(name = "nombre", nullable = false, length = 100, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "equipamiento", length = 100)
    private String equipamiento;

    @Column(name = "imagen_ruta", length = 255)
    private String imagenRuta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_estado", nullable = false)
    private TipoEstado tipoEstado;

    protected Ejercicio() { }

    public Ejercicio(GrupoMuscular grupo, NivelExperiencia nivel, TipoMedicion medicion,
                     String nombre, String descripcion, String equipamiento,
                     String imagenRuta, TipoEstado estado) {
        this.grupoMuscular = grupo;
        this.nivelExperiencia = nivel;
        this.tipoMedicion = medicion;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.equipamiento = equipamiento;
        this.imagenRuta = imagenRuta;
        this.tipoEstado = estado;
    }

    public void actualizar(GrupoMuscular grupo, NivelExperiencia nivel, TipoMedicion medicion,
                           String nombre, String descripcion, String equipamiento, String imagenRuta) {
        this.grupoMuscular = grupo;
        this.nivelExperiencia = nivel;
        this.tipoMedicion = medicion;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.equipamiento = equipamiento;
        this.imagenRuta = imagenRuta;
    }

    public void cambiarEstado(TipoEstado estado) { this.tipoEstado = estado; }

    public Integer getIdEjercicio() { return idEjercicio; }
    public GrupoMuscular getGrupoMuscular() { return grupoMuscular; }
    public NivelExperiencia getNivelExperiencia() { return nivelExperiencia; }
    public TipoMedicion getTipoMedicion() { return tipoMedicion; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getEquipamiento() { return equipamiento; }
    public String getImagenRuta() { return imagenRuta; }
    public TipoEstado getTipoEstado() { return tipoEstado; }
}
