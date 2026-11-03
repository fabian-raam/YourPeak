package com.yourpeak.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "alimento")
public class Alimento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alimento")
    private Integer idAlimento;

    @Column(name = "nombre", nullable = false, length = 120, unique = true)
    private String nombre;

    @Column(name = "porcion_base_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal porcionBaseG;

    @Column(name = "kcal", nullable = false, precision = 7, scale = 2)
    private BigDecimal kcal;

    @Column(name = "proteina_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal proteinaG;

    @Column(name = "carbohidrato_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal carbohidratoG;

    @Column(name = "grasa_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal grasaG;

    protected Alimento() { }

    public Alimento(String nombre, BigDecimal porcionBaseG, BigDecimal kcal,
                    BigDecimal proteinaG, BigDecimal carbohidratoG, BigDecimal grasaG) {
        this.nombre = nombre;
        this.porcionBaseG = porcionBaseG;
        this.kcal = kcal;
        this.proteinaG = proteinaG;
        this.carbohidratoG = carbohidratoG;
        this.grasaG = grasaG;
    }

    public void actualizar(String nombre, BigDecimal porcionBaseG, BigDecimal kcal,
                           BigDecimal proteinaG, BigDecimal carbohidratoG, BigDecimal grasaG) {
        this.nombre = nombre;
        this.porcionBaseG = porcionBaseG;
        this.kcal = kcal;
        this.proteinaG = proteinaG;
        this.carbohidratoG = carbohidratoG;
        this.grasaG = grasaG;
    }

    public Integer getIdAlimento() { return idAlimento; }
    public String getNombre() { return nombre; }
    public BigDecimal getPorcionBaseG() { return porcionBaseG; }
    public BigDecimal getKcal() { return kcal; }
    public BigDecimal getProteinaG() { return proteinaG; }
    public BigDecimal getCarbohidratoG() { return carbohidratoG; }
    public BigDecimal getGrasaG() { return grasaG; }
}
