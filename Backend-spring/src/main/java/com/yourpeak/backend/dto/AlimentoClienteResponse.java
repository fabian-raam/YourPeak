package com.yourpeak.backend.dto;

import java.math.BigDecimal;

/** Datos del catálogo necesarios para buscar y registrar alimentos como cliente. */
public record AlimentoClienteResponse(
        Integer idAlimento, String nombre, BigDecimal porcionBaseG, BigDecimal kcal,
        BigDecimal proteinaG, BigDecimal carbohidratoG, BigDecimal grasaG
) { }
