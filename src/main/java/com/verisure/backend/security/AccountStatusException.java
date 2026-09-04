package com.verisure.backend.security;

import org.springframework.security.core.AuthenticationException;

import com.verisure.backend.exception.ErrorCode;

import lombok.Getter;

/**
 * Las credenciales eran correctas, pero la cuenta no está en condiciones de
 * entrar: falta verificar el correo, falta la aprobación de la Fundación o fue
 * rechazada.
 *
 * <p>Lleva su {@link ErrorCode} dentro —igual que {@code DomainException}— para
 * que quien la capture sepa cuál de los tres motivos es sin mirar el mensaje.
 *
 * <p>Extiende {@code AuthenticationException} para que Spring Security la trate
 * como un fallo de autenticación y llegue a
 * {@code JWTAuthentication.unsuccessfulAuthentication}.
 */
@Getter
public class AccountStatusException extends AuthenticationException {

    private final ErrorCode errorCode;

    public AccountStatusException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
