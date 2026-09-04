package com.verisure.backend.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.entity.enums.UserStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Traduce la entidad {@link User} a lo que Spring Security entiende.
 *
 * <p><b>El prefijo {@code ROLE_} no es decorativo.</b> {@code hasRole("PARTNER")}
 * busca internamente la autoridad {@code ROLE_PARTNER}. Si aquí se expusiera
 * {@code PARTNER} a secas, ninguna regla casaría y <b>todo devolvería 403</b> sin
 * ningún error que lo explicara. Quien prefiera escribir la autoridad literal usa
 * {@code hasAuthority}, pero en este proyecto se usa {@code hasRole}.
 */
@Getter
@RequiredArgsConstructor
public class UserDetail implements UserDetails {

    public static final String ROLE_PREFIX = "ROLE_";

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authoritiesOf(user.getRole());
    }

    /** Misma construcción que usa el filtro, que saca el rol del token. */
    public static List<GrantedAuthority> authoritiesOf(Role role) {
        return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()));
    }

    @Override
    public String getUsername() {
        return user.getEmail();   // el correo es el identificador de entrada
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Solo entran las cuentas activas.
     *
     * <p>Cuál de los tres motivos impide entrar lo distingue
     * {@link CustomAuthenticationManager}, porque este método es un booleano y no
     * puede decirlo.
     */
    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
