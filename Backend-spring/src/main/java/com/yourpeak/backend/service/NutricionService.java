package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.AlimentoClienteResponse;
import com.yourpeak.backend.dto.IngestaResponse;
import com.yourpeak.backend.dto.RegistrarIngestaRequest;
import com.yourpeak.backend.dto.ResumenNutricionalResponse;
import com.yourpeak.backend.model.Alimento;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.RegistroIngesta;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.AlimentoRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.RegistroIngestaRepository;
import com.yourpeak.backend.repository.ResumenClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** Catálogo de lectura y bitácora diaria de alimentos para clientes activos. */
@Service
public class NutricionService {
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final ClienteRepository clienteRepository;
    private final AdministradorRepository administradorRepository;
    private final AlimentoRepository alimentoRepository;
    private final RegistroIngestaRepository ingestaRepository;
    private final ResumenClienteRepository resumenRepository;

    public NutricionService(ClienteRepository clienteRepository,
                            AdministradorRepository administradorRepository,
                            AlimentoRepository alimentoRepository,
                            RegistroIngestaRepository ingestaRepository,
                            ResumenClienteRepository resumenRepository) {
        this.clienteRepository = clienteRepository;
        this.administradorRepository = administradorRepository;
        this.alimentoRepository = alimentoRepository;
        this.ingestaRepository = ingestaRepository;
        this.resumenRepository = resumenRepository;
    }

    @Transactional(readOnly = true)
    public List<AlimentoClienteResponse> listarAlimentos(Long idUsuario) {
        validarClienteActivo(idUsuario);
        return alimentoRepository.findAllByOrderByNombreAsc().stream()
                .map(alimento -> new AlimentoClienteResponse(
                        alimento.getIdAlimento(), alimento.getNombre(), alimento.getPorcionBaseG(),
                        alimento.getKcal(), alimento.getProteinaG(), alimento.getCarbohidratoG(),
                        alimento.getGrasaG()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumenNutricionalResponse consultarDia(Long idUsuario, LocalDate fecha) {
        Cliente cliente = validarClienteActivo(idUsuario);
        LocalDate dia = fecha == null ? LocalDate.now(ZONA_LIMA) : fecha;
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fin = dia.plusDays(1).atStartOfDay();
        List<IngestaResponse> registros = ingestaRepository
                .findAllByCliente_IdClienteAndFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(
                        cliente.getIdCliente(), inicio, fin)
                .stream().map(this::respuesta).toList();

        return new ResumenNutricionalResponse(dia,
                sumar(registros, IngestaResponse::kcal),
                sumar(registros, IngestaResponse::proteinaG),
                sumar(registros, IngestaResponse::carbohidratoG),
                sumar(registros, IngestaResponse::grasaG), registros,
                resumenRepository.buscarMeta(cliente.getIdCliente(), dia).orElse(null));
    }

    @Transactional
    public IngestaResponse registrar(Long idUsuario, RegistrarIngestaRequest request) {
        Cliente cliente = validarClienteActivo(idUsuario);
        Alimento alimento = buscarAlimento(request.idAlimento());
        return respuesta(ingestaRepository.save(
                new RegistroIngesta(cliente, alimento, request.cantidadG())));
    }

    @Transactional
    public IngestaResponse actualizar(Long idUsuario, Long idIngesta,
                                      RegistrarIngestaRequest request) {
        Cliente cliente = validarClienteActivo(idUsuario);
        RegistroIngesta ingesta = ingestaRepository
                .findByIdIngestaAndCliente_IdCliente(idIngesta, cliente.getIdCliente())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró ese consumo para este cliente."));
        ingesta.actualizar(buscarAlimento(request.idAlimento()), request.cantidadG());
        return respuesta(ingesta);
    }

    @Transactional
    public void eliminar(Long idUsuario, Long idIngesta) {
        Cliente cliente = validarClienteActivo(idUsuario);
        RegistroIngesta ingesta = ingestaRepository
                .findByIdIngestaAndCliente_IdCliente(idIngesta, cliente.getIdCliente())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró ese consumo para este cliente."));
        ingestaRepository.delete(ingesta);
    }

    private Cliente validarClienteActivo(Long idUsuario) {
        if (idUsuario == null || administradorRepository.existsByUsuario_IdUsuario(idUsuario)) {
            throw accesoDenegado();
        }
        Cliente cliente = clienteRepository.findByUsuario_IdUsuario(idUsuario)
                .orElseThrow(NutricionService::accesoDenegado);
        if (!"ACTIVO".equalsIgnoreCase(cliente.getTipoEstado().getNombre())) {
            throw accesoDenegado();
        }
        return cliente;
    }

    private Alimento buscarAlimento(Integer idAlimento) {
        return alimentoRepository.findById(idAlimento).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el alimento seleccionado."));
    }

    private IngestaResponse respuesta(RegistroIngesta ingesta) {
        Alimento alimento = ingesta.getAlimento();
        BigDecimal factor = ingesta.getCantidadG().divide(alimento.getPorcionBaseG(), 8,
                RoundingMode.HALF_UP);
        return new IngestaResponse(ingesta.getIdIngesta(), alimento.getIdAlimento(),
                alimento.getNombre(), ingesta.getCantidadG(), ingesta.getFechaHora(),
                escalar(alimento.getKcal(), factor), escalar(alimento.getProteinaG(), factor),
                escalar(alimento.getCarbohidratoG(), factor), escalar(alimento.getGrasaG(), factor));
    }

    private BigDecimal escalar(BigDecimal valorPorBase, BigDecimal factor) {
        return valorPorBase.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumar(List<IngestaResponse> registros,
                              java.util.function.Function<IngestaResponse, BigDecimal> campo) {
        return registros.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static ResponseStatusException accesoDenegado() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo una cuenta cliente activa puede gestionar su nutrición.");
    }
}
