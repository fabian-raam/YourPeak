package com.yourpeak.backend.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Ejecuta la transición ACTIVO -> INACTIVO al vencer los períodos de membresía. */
@Component
public class MembresiaScheduler {

    private final MembresiaService membresiaService;

    public MembresiaScheduler(MembresiaService membresiaService) {
        this.membresiaService = membresiaService;
    }

    /** Al iniciar, corrige también los vencimientos ocurridos mientras Spring estuvo apagado. */
    @EventListener(ApplicationReadyEvent.class)
    public void procesarAlIniciar() {
        membresiaService.desactivarClientesVencidos();
    }

    /** El período vence al terminar su fecha; la cuenta se inactiva a medianoche de Lima. */
    @Scheduled(cron = "0 0 0 * * *", zone = "America/Lima")
    public void procesarCadaMedianoche() {
        membresiaService.desactivarClientesVencidos();
    }
}
