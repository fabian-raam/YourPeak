package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.ResumenNutricionalResponse.Meta;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.RegistroFisico;
import com.yourpeak.backend.repository.MetaNutricionalRepository;
import com.yourpeak.backend.repository.NivelActividadRepository;
import com.yourpeak.backend.repository.TipoObjetivoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Map;

/** Calcula metas diarias estimadas y conserva su vigencia histórica. */
@Service
public class MetaNutricionalService {
    private static final BigDecimal CALORIAS_PROTEINA = new BigDecimal("4");
    private static final BigDecimal CALORIAS_CARBOHIDRATO = new BigDecimal("4");
    private static final BigDecimal CALORIAS_GRASA = new BigDecimal("9");
    private static final BigDecimal PROTEINA_POR_KILO = new BigDecimal("1.6");
    private static final BigDecimal PROPORCION_CALORIAS_GRASA = new BigDecimal("0.25");
    private static final Map<String, BigDecimal> FACTORES_ACTIVIDAD = Map.of(
            "sedentario", new BigDecimal("1.20"),
            "ligero", new BigDecimal("1.375"),
            "moderado", new BigDecimal("1.55"),
            "intenso", new BigDecimal("1.725")
    );

    private final MetaNutricionalRepository metaRepository;
    private final TipoObjetivoRepository objetivoRepository;
    private final NivelActividadRepository actividadRepository;

    public MetaNutricionalService(MetaNutricionalRepository metaRepository,
                                  TipoObjetivoRepository objetivoRepository,
                                  NivelActividadRepository actividadRepository) {
        this.metaRepository = metaRepository;
        this.objetivoRepository = objetivoRepository;
        this.actividadRepository = actividadRepository;
    }

    /**
     * Actualiza la meta después de guardar el perfil o una medición de peso.
     * La primera meta rige hoy; los cambios posteriores empiezan mañana.
     */
    @Transactional
    public void actualizar(Cliente cliente, RegistroFisico medicion, LocalDate hoy) {
        if (!perfilListo(cliente, medicion)) return;

        Meta nuevaMeta = calcular(cliente, medicion, hoy);
        metaRepository.eliminarMetasFuturas(cliente.getIdCliente(), hoy);

        var metaVigente = metaRepository.buscarVigente(cliente.getIdCliente(), hoy);
        if (metaVigente.isPresent() && iguales(metaVigente.get(), nuevaMeta)) return;

        LocalDate inicioNuevaMeta = metaVigente.isPresent() ? hoy.plusDays(1) : hoy;
        metaRepository.cerrarMetasAnteriores(cliente.getIdCliente(), inicioNuevaMeta);
        metaRepository.guardar(cliente.getIdCliente(), nuevaMeta, inicioNuevaMeta);
    }

    private Meta calcular(Cliente cliente, RegistroFisico medicion, LocalDate hoy) {
        var nacimiento = cliente.getUsuario().getFechaNacimiento();
        int edad = Period.between(nacimiento, hoy).getYears();
        BigDecimal peso = medicion.getPesoKg();
        BigDecimal talla = medicion.getTallaCm();

        BigDecimal ajusteSexo = switch (cliente.getSexo()) {
            case "HOMBRE" -> new BigDecimal("5");
            case "MUJER" -> new BigDecimal("-161");
            default -> throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Registra un sexo válido en tu perfil para calcular la meta nutricional.");
        };

        BigDecimal reposo = peso.multiply(BigDecimal.TEN)
                .add(talla.multiply(new BigDecimal("6.25")))
                .subtract(BigDecimal.valueOf(edad).multiply(new BigDecimal("5")))
                .add(ajusteSexo);

        String actividad = actividadRepository.findById(cliente.getIdNivelActividad())
                .map(nivel -> normalizar(nivel.getNombre()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Selecciona un nivel de actividad válido en tu perfil."));
        BigDecimal factorActividad = FACTORES_ACTIVIDAD.get(actividad);
        if (factorActividad == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El nivel de actividad no tiene un factor configurado para la meta.");
        }

        BigDecimal mantenimiento = reposo.multiply(factorActividad);
        String objetivo = objetivoRepository.findById(cliente.getIdTipoObjetivo())
                .map(tipo -> normalizar(tipo.getNombre()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Selecciona un objetivo válido en tu perfil."));
        BigDecimal caloriasObjetivo = switch (objetivo) {
            case "perder grasa" -> mantenimiento.multiply(new BigDecimal("0.85"));
            case "ganar masa muscular" -> mantenimiento.multiply(new BigDecimal("1.10"));
            case "mantener peso" -> mantenimiento;
            default -> throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El objetivo no tiene un ajuste configurado para la meta.");
        };

        BigDecimal kcal = redondear(caloriasObjetivo);
        BigDecimal proteina = redondear(peso.multiply(PROTEINA_POR_KILO));
        BigDecimal grasa = redondear(kcal.multiply(PROPORCION_CALORIAS_GRASA)
                .divide(CALORIAS_GRASA, 8, RoundingMode.HALF_UP));
        BigDecimal caloriasRestantes = kcal
                .subtract(proteina.multiply(CALORIAS_PROTEINA))
                .subtract(grasa.multiply(CALORIAS_GRASA));
        if (caloriasRestantes.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Los datos del perfil no permiten calcular una meta nutricional válida.");
        }
        BigDecimal carbohidrato = redondear(caloriasRestantes
                .divide(CALORIAS_CARBOHIDRATO, 8, RoundingMode.HALF_UP));

        return new Meta(kcal, proteina, carbohidrato, grasa);
    }

    private boolean perfilListo(Cliente cliente, RegistroFisico medicion) {
        return cliente.getUsuario().getFechaNacimiento() != null
                && cliente.getSexo() != null
                && cliente.getIdTipoObjetivo() != null
                && cliente.getIdNivelActividad() != null
                && medicion != null;
    }

    private boolean iguales(Meta a, Meta b) {
        return a.kcal().compareTo(b.kcal()) == 0
                && a.proteinaG().compareTo(b.proteinaG()) == 0
                && a.carbohidratoG().compareTo(b.carbohidratoG()) == 0
                && a.grasaG().compareTo(b.grasaG()) == 0;
    }

    private BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
