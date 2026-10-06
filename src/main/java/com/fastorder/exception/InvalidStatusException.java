package com.fastorder.exception;

public class InvalidStatusException extends RuntimeException {

    public InvalidStatusException(String mensaje) {
        super(mensaje);
    }
}
