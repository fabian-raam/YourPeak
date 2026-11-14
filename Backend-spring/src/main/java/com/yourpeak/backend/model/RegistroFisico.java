package com.yourpeak.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "registro_fisico")
public class RegistroFisico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro_fisico")
    private Long idRegistroFisico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "peso_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "talla_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal tallaCm;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "observacion", length = 500)
    private String observacion;

    protected RegistroFisico() { }

    public RegistroFisico(Cliente cliente, BigDecimal pesoKg, BigDecimal tallaCm) {
        this.cliente = cliente;
        this.pesoKg = pesoKg;
        this.tallaCm = tallaCm;
    }

    @PrePersist
    private void asignarFechaRegistro() {
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getIdRegistroFisico() { return idRegistroFisico; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public BigDecimal getTallaCm() { return tallaCm; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
