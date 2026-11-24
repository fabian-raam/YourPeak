package com.yourpeak.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTests {

    private final ApiExceptionHandler manejador = new ApiExceptionHandler();

    @Test
    void conservaElMotivoDeUnErrorDeServicio() {
        ProblemDetail problema = manejador.manejarEstado(new ResponseStatusException(
                HttpStatus.CONFLICT, "Marca al menos un día disponible antes de generar tu rutina."));

        assertEquals(409, problema.getStatus());
        assertEquals("Marca al menos un día disponible antes de generar tu rutina.", problema.getDetail());
    }

    @Test
    void usaUnMensajeGeneralSiNoHayMotivo() {
        ProblemDetail problema = manejador.manejarEstado(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertEquals(404, problema.getStatus());
        assertEquals("No se pudo completar la solicitud.", problema.getDetail());
    }
}
