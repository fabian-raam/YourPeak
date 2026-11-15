package com.yourpeak.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Un alimento consumido por un cliente en una fecha y hora concretas. */
@Entity
@Table(name = "registro_ingesta")
public class RegistroIngesta {
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ingesta")
    private Long idIngesta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_alimento", nullable = false)
    private Alimento alimento;

    @Column(name = "cantidad_g", nullable = false, precision = 7, scale = 2)
    private BigDecimal cantidadG;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    protected RegistroIngesta() { }

    public RegistroIngesta(Cliente cliente, Alimento alimento, BigDecimal cantidadG) {
        this.cliente = cliente;
        this.alimento = alimento;
        this.cantidadG = cantidadG;
        this.fechaHora = LocalDateTime.now(ZONA_LIMA);
    }

    public void actualizar(Alimento alimento, BigDecimal cantidadG) {
        this.alimento = alimento;
        this.cantidadG = cantidadG;
    }

    public Long getIdIngesta() { return idIngesta; }
    public Cliente getCliente() { return cliente; }
    public Alimento getAlimento() { return alimento; }
    public BigDecimal getCantidadG() { return cantidadG; }
    public LocalDateTime getFechaHora() { return fechaHora; }
}
