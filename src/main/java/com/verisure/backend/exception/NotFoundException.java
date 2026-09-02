package com.verisure.backend.exception;

/**
 * Recurso inexistente.
 *
 * <p>Es la única que no está en {@link ErrorCode}: 404 no es un código de
 * negocio, es la respuesta a pedir algo que no hay. El manejador global la
 * traduce a {@code NOT_FOUND}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String entity, Object id) {
        return new NotFoundException("No existe %s con id %s".formatted(entity, id));
    }
}
