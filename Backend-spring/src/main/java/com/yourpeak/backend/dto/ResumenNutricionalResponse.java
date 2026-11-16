package com.yourpeak.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Resumen de lo consumido por fecha local, con sus registros detallados. */
public record ResumenNutricionalResponse(
        LocalDate fecha, BigDecimal kcal, BigDecimal proteinaG,
        BigDecimal carbohidratoG, BigDecimal grasaG, List<IngestaResponse> ingestas, Meta meta
) {
    public record Meta(BigDecimal kcal, BigDecimal proteinaG, BigDecimal carbohidratoG, BigDecimal grasaG) { }
}
