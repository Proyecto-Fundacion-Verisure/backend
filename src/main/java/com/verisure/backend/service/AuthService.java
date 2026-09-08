package com.verisure.backend.service;

import com.verisure.backend.dto.auth.RegisterPartnerRequest;
import com.verisure.backend.dto.user.UserResponse;
/**
 * La autenticación de sesión que no vive en la cadena de seguridad.
 *
 * <p>Solo tiene {@code logout}: el login lo atiende {@code JWTAuthentication},
 * dentro del filtro, porque devuelve el token en su respuesta.
 */
public interface AuthService {

    /**
     * Perfil de quien hace la petición. El correo llega ya validado por el
     * token —lo comprobó
     * {@link com.verisure.backend.security.filter.JWTAuthorization} antes de
     * llegar al controlador—; aquí solo falta traer la fila.
     */
    UserResponse me(String email);

    /**
     * Registro conjunuto de una entidad colaboradora y su persona de contacto.
     *
     * <p>El correo es el identificador único de la persona: no puede estar ya en
     * ninguna cuenta, ni en la misma entidad ni en otra. Si el CIF no existe, se
     * crean el {@code Partner} en {@code PENDING} y el usuario en
     * {@code PENDING_VERIFICATION}. Si el CIF ya existe, no se crea una entidad
     * nueva: el usuario se asocia a la que hay y queda igualmente pendiente de
     * aprobación. La deduplicación evita que dos personas de una misma entidad
     * acaben en dos partners casi homónimos, partiendo sus horas en el ranking.
     *
     * <p>No se emite token: la cuenta nace sin verificar, y el login ya la bloquea
     * hasta que confirme el correo ({@code ACCOUNT_NOT_VERIFIED}).
     *
     * @throws com.verisure.backend.exception.DomainException con
     * {@code CIF_ALREADY_REGISTERED} si el correo ya tiene cuenta en esa entidad,
     * o {@code EMAIL_ALREADY_REGISTERED} si el correo ya está registrado en
     * cualquier cuenta.
     */
    UserResponse registerPartner(RegisterPartnerRequest request);

    /**
     * Cierra una sesión en el servidor. <b>No revoca el JWT</b>: un token es
     * sin estado y no hay fila que borrar; la caducidad de dos horas es la
     * política de revocación. El cierre real ocurre en el cliente, que descarta
     * el token. Aquí solo queda constancia en la traza.
     */
    void logout(String email);
}
