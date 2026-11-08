package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.DiaDisponibilidadRequest;
import com.yourpeak.backend.dto.DiaDisponibilidadResponse;
import com.yourpeak.backend.dto.DisponibilidadRequest;
import com.yourpeak.backend.dto.DisponibilidadResponse;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.DisponibilidadCliente;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.DisponibilidadClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Consulta y actualiza los horarios del cliente autenticado por Django. */
@Service
public class DisponibilidadService {

    private final ClienteRepository clienteRepository;
    private final AdministradorRepository administradorRepository;
    private final DisponibilidadClienteRepository disponibilidadRepository;

    public DisponibilidadService(
            ClienteRepository clienteRepository,
            AdministradorRepository administradorRepository,
            DisponibilidadClienteRepository disponibilidadRepository
    ) {
        this.clienteRepository = clienteRepository;
        this.administradorRepository = administradorRepository;
        this.disponibilidadRepository = disponibilidadRepository;
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse consultar(Long idUsuario) {
        Cliente cliente = buscarClienteActivo(idUsuario);
        Map<Integer, DisponibilidadCliente> guardados = new HashMap<>();
        disponibilidadRepository.findAllByCliente_IdClienteOrderByDiaSemanaAsc(cliente.getIdCliente())
                .forEach(dia -> guardados.put(dia.getDiaSemana(), dia));

        List<DiaDisponibilidadResponse> dias = new ArrayList<>(7);
        for (int dia = 1; dia <= 7; dia++) {
            DisponibilidadCliente horario = guardados.get(dia);
            dias.add(horario == null
                    ? new DiaDisponibilidadResponse(dia, false, null, null)
                    : new DiaDisponibilidadResponse(
                            dia, true, horario.getHoraInicio(), horario.getHoraFin()
                    ));
        }
        return new DisponibilidadResponse(dias);
    }

    @Transactional
    public DisponibilidadResponse actualizar(Long idUsuario, DisponibilidadRequest request) {
        Cliente cliente = buscarClienteActivo(idUsuario);
        validarDias(request.dias());

        disponibilidadRepository.deleteAllByCliente_IdCliente(cliente.getIdCliente());
        List<DisponibilidadCliente> nuevosHorarios = request.dias().stream()
                .filter(DiaDisponibilidadRequest::disponible)
                .map(dia -> new DisponibilidadCliente(
                        cliente, dia.diaSemana(), dia.horaInicio(), dia.horaFin()
                ))
                .toList();
        disponibilidadRepository.saveAll(nuevosHorarios);

        return consultar(idUsuario);
    }

    /** Resuelve el cliente desde el usuario validado; nunca acepta un idCliente del navegador. */
    private Cliente buscarClienteActivo(Long idUsuario) {
        if (administradorRepository.existsByUsuario_IdUsuario(idUsuario)) {
            throw accesoDenegado();
        }

        Cliente cliente = clienteRepository.findByUsuario_IdUsuario(idUsuario)
                .orElseThrow(DisponibilidadService::accesoDenegado);
        if (!"ACTIVO".equalsIgnoreCase(cliente.getTipoEstado().getNombre())) {
            throw accesoDenegado();
        }
        return cliente;
    }

    private void validarDias(List<DiaDisponibilidadRequest> dias) {
        if (dias == null || dias.size() != 7) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Envía exactamente los siete días de la semana."
            );
        }

        boolean[] vistos = new boolean[8];
        for (DiaDisponibilidadRequest dia : dias) {
            Integer numeroDia = dia.diaSemana();
            if (numeroDia == null || numeroDia < 1 || numeroDia > 7 || vistos[numeroDia]) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cada día debe aparecer una sola vez, usando valores del 1 al 7."
                );
            }
            vistos[numeroDia] = true;

            if (Boolean.TRUE.equals(dia.disponible())) {
                LocalTime inicio = dia.horaInicio();
                LocalTime fin = dia.horaFin();
                if (inicio == null || fin == null || !fin.isAfter(inicio)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Los días disponibles requieren horas válidas y la hora final debe ser posterior."
                    );
                }
            } else if (dia.horaInicio() != null || dia.horaFin() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Los días no disponibles no deben incluir horas."
                );
            }
        }
    }

    private static ResponseStatusException accesoDenegado() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo una cuenta cliente activa puede gestionar su disponibilidad."
        );
    }
}
