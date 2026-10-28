package com.yourpeak.backend.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> campos = new TreeMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error ->
                campos.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage() == null
                                ? "Valor inválido"
                                : error.getDefaultMessage()
                )
        );

        ProblemDetail problema = problema(
                HttpStatus.BAD_REQUEST,
                "Datos inválidos",
                "Revisa los campos del registro."
        );

        problema.setProperty("campos", campos);
        return problema;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail manejarValidacionServicio() {
        return problema(
                HttpStatus.BAD_REQUEST,
                "Datos inválidos",
                "Los datos no cumplen las condiciones del registro."
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail manejarJsonInvalido() {
        return problema(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "Envía un cuerpo JSON válido con los datos del registro."
        );
    }

    @ExceptionHandler(CorreoRegistradoException.class)
    public ProblemDetail manejarCorreoRegistrado() {
        return conflictoCorreo();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail manejarIntegridad(
            DataIntegrityViolationException exception
    ) {
        Throwable causa = exception;

        while (causa != null) {
            if (causa instanceof
                    org.hibernate.exception.ConstraintViolationException error) {

                String restriccion = error.getConstraintName();

                if (restriccion != null
                        && (restriccion.equals("uq_usuario_correo")
                        || restriccion.endsWith(".uq_usuario_correo"))) {
                    return conflictoCorreo();
                }
                if (restriccion != null
                        && (restriccion.equals("uq_plan_membresia_nombre")
                        || restriccion.endsWith(".uq_plan_membresia_nombre"))) {
                    return conflictoPlanMembresia();
                }
            }

            causa = causa.getCause();
        }

        return errorInterno();
    }

    @ExceptionHandler({
            IllegalStateException.class,
            DataAccessException.class
    })
    public ProblemDetail manejarErrorInterno() {
        return errorInterno();
    }

    private ProblemDetail conflictoCorreo() {
        return problema(
                HttpStatus.CONFLICT,
                "Registro no disponible",
                "No se puede completar el registro con ese correo."
        );
    }

    private ProblemDetail conflictoPlanMembresia() {
        return problema(
                HttpStatus.CONFLICT,
                "Plan no disponible",
                "Ya existe un plan de membresía con ese nombre."
        );
    }

    private ProblemDetail errorInterno() {
        return problema(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno",
                "No se pudo completar el registro. Inténtalo más tarde."
        );
    }

    private ProblemDetail problema(
            HttpStatus status,
            String titulo,
            String detalle
    ) {
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(status, detalle);

        problema.setTitle(titulo);
        return problema;
    }
}
