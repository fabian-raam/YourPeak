package com.yourpeak.backend.exception;

public class CorreoRegistradoException extends RuntimeException {

    public CorreoRegistradoException() {
        super("No se puede completar el registro con ese correo");
    }
}