package com.verisure.backend.exception;

import lombok.Getter;

/**
 * Excepción de negocio: lleva su {@link ErrorCode} dentro.
 *
 * <p>Es lo que permite que {@link GlobalExceptionHandler} tenga un solo
 * {@code @ExceptionHandler} para todos los códigos, en vez de una clase de
 * excepción y un manejador por cada uno.
 *
 * <p>Extiende {@code RuntimeException} para que Spring deshaga la transacción
 * sin necesidad de {@code rollbackFor}. Por eso los servicios tienen que usar
 * {@code org.springframework.transaction.annotation.Transactional} y no la de
 * {@code jakarta}, que solo deshace con excepciones comprobadas.
 */
@Getter
public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    public DomainException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
