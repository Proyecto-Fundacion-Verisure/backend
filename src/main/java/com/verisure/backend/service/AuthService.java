package com.verisure.backend.service;

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
     * Cierra una sesión en el servidor. <b>No revoca el JWT</b>: un token es
     * sin estado y no hay fila que borrar; la caducidad de dos horas es la
     * política de revocación. El cierre real ocurre en el cliente, que descarta
     * el token. Aquí solo queda constancia en la traza.
     */
    void logout(String email);
}
