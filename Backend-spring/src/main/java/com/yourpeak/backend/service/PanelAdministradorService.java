package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.DetalleEntrenamientoAdministradorResponse;
import com.yourpeak.backend.dto.EntrenamientoAdministradorResponse;
import com.yourpeak.backend.dto.ResumenPanelAdministradorResponse;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.PanelAdministradorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Coordina las consultas administrativas y verifica el rol antes de leer información. */
@Service
public class PanelAdministradorService {
    private final AdministradorRepository administradorRepository;
    private final PanelAdministradorRepository panelRepository;

    public PanelAdministradorService(AdministradorRepository administradorRepository,
                                     PanelAdministradorRepository panelRepository) {
        this.administradorRepository = administradorRepository;
        this.panelRepository = panelRepository;
    }

    @Transactional(readOnly = true)
    public ResumenPanelAdministradorResponse consultarResumen(Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        return panelRepository.consultarResumen();
    }

    @Transactional(readOnly = true)
    public List<EntrenamientoAdministradorResponse> listarEntrenamientos(Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        return panelRepository.listarEntrenamientos();
    }

    @Transactional(readOnly = true)
    public DetalleEntrenamientoAdministradorResponse consultarEntrenamiento(
            Long idCliente, Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        return panelRepository.consultarEntrenamiento(idCliente)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un cliente activo o inactivo con ese identificador."
                ));
    }

    private void verificarAdministrador(Long idUsuarioAdministrador) {
        if (idUsuarioAdministrador == null
                || !administradorRepository.existsByUsuario_IdUsuario(idUsuarioAdministrador)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador puede consultar esta información."
            );
        }
    }
}
