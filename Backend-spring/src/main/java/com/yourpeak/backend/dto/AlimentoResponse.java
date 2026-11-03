package com.yourpeak.backend.dto;

import java.math.BigDecimal;

public record AlimentoResponse(
        Integer idAlimento,
        String nombre,
        BigDecimal porcionBaseG,
        BigDecimal kcal,
        BigDecimal proteinaG,
        BigDecimal carbohidratoG,
        BigDecimal grasaG
) { }
