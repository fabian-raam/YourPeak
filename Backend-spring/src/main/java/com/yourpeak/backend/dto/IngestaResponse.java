package com.yourpeak.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IngestaResponse(
        Long idIngesta, Integer idAlimento, String alimento, BigDecimal cantidadG,
        LocalDateTime fechaHora, BigDecimal kcal, BigDecimal proteinaG,
        BigDecimal carbohidratoG, BigDecimal grasaG
) { }
