package com.verisure.backend.exception;

/**
 * Formato de archivo no admitido en un multipart.
 *
 * <p>Igual que 404, 413 y 500, 415 no es un código de negocio y no está en
 * {@link ErrorCode}: el manejador global lo traduce a la forma única
 * {@link ApiError} con {@code UNSUPPORTED_MEDIA_TYPE}. Por eso esta clase es
 * una excepción dedicada y no una {@link DomainException}.
 */
public class UnsupportedMediaTypeException extends RuntimeException {

    public UnsupportedMediaTypeException(String message) {
        super(message);
    }
}