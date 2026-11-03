package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.AlimentoRequest;
import com.yourpeak.backend.dto.AlimentoResponse;
import com.yourpeak.backend.model.Alimento;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.AlimentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AlimentoService {
    private final AdministradorRepository administradorRepository;
    private final AlimentoRepository alimentoRepository;

    public AlimentoService(AdministradorRepository administradorRepository,
                           AlimentoRepository alimentoRepository) {
        this.administradorRepository = administradorRepository;
        this.alimentoRepository = alimentoRepository;
    }

    @Transactional(readOnly = true)
    public List<AlimentoResponse> listar(Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        return alimentoRepository.findAllByOrderByNombreAsc().stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public AlimentoResponse obtener(Integer idAlimento, Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        return respuesta(buscar(idAlimento));
    }

    @Transactional
    public AlimentoResponse crear(AlimentoRequest request, Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        String nombre = normalizarNombre(request.nombre());
        verificarNombreDisponible(nombre, null);
        return respuesta(alimentoRepository.save(new Alimento(
                nombre, request.porcionBaseG(), request.kcal(), request.proteinaG(),
                request.carbohidratoG(), request.grasaG()
        )));
    }

    @Transactional
    public AlimentoResponse actualizar(Integer idAlimento, AlimentoRequest request,
                                       Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        Alimento alimento = buscar(idAlimento);
        String nombre = normalizarNombre(request.nombre());
        verificarNombreDisponible(nombre, idAlimento);
        alimento.actualizar(nombre, request.porcionBaseG(), request.kcal(), request.proteinaG(),
                request.carbohidratoG(), request.grasaG());
        return respuesta(alimento);
    }

    private void verificarAdministrador(Long idUsuario) {
        if (idUsuario == null || !administradorRepository.existsByUsuario_IdUsuario(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un administrador puede gestionar el catálogo de alimentos.");
        }
    }

    private Alimento buscar(Integer idAlimento) {
        return alimentoRepository.findById(idAlimento).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No se encontró el alimento solicitado."
        ));
    }

    private void verificarNombreDisponible(String nombre, Integer idActual) {
        boolean existe = idActual == null
                ? alimentoRepository.existsByNombreIgnoreCase(nombre)
                : alimentoRepository.existsByNombreIgnoreCaseAndIdAlimentoNot(nombre, idActual);
        if (existe) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya existe un alimento con ese nombre.");
    }

    private String normalizarNombre(String nombre) { return nombre.strip(); }

    private AlimentoResponse respuesta(Alimento alimento) {
        return new AlimentoResponse(alimento.getIdAlimento(), alimento.getNombre(), alimento.getPorcionBaseG(),
                alimento.getKcal(), alimento.getProteinaG(), alimento.getCarbohidratoG(), alimento.getGrasaG());
    }
}
