package com.verisure.backend.security.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.security.JwtService;
import com.verisure.backend.security.UserDetail;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Valida el token en <b>cada</b> petición y puebla el {@code SecurityContext}.
 *
 * <p><b>Nunca lanza.</b> Las rutas públicas pasan también por aquí; si reventara
 * con una petición sin cabecera, el formulario público de propuestas dejaría de
 * funcionar. Sin token, o con uno inválido, no autentica a nadie y deja que la
 * cadena decida si esa ruta lo necesitaba.
 *
 * <p>El rol se saca <b>del propio token</b>, sin consultar la base de datos: es
 * lo que hace que JWT no cueste una consulta por petición. La contrapartida es
 * que un cambio de rol o un rechazo de cuenta no tienen efecto hasta que el token
 * caduca, como mucho dos horas.
 */
@RequiredArgsConstructor
public class JWTAuthorization extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);

        if (header != null && header.startsWith(PREFIX)) {
            jwtService.validate(header.substring(PREFIX.length()))
                      .ifPresent(jwt -> authenticate(jwt, request));
        }

        filterChain.doFilter(request, response);   // sigue siempre
    }

    private void authenticate(DecodedJWT jwt, HttpServletRequest request) {
        String email = jwt.getSubject();
        String role = jwt.getClaim(JwtService.CLAIM_ROLE).asString();
        if (email == null || role == null) {
            return;   // token bien firmado pero incompleto: no se autentica a nadie
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                email, null, UserDetail.authoritiesOf(Role.valueOf(role)));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
