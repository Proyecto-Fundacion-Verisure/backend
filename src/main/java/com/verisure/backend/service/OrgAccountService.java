package com.verisure.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.verisure.backend.dto.orgaccount.OrgAccountRow;

/**
 * La relación de cuentas de entidad y su aprobación o rechazo · {@code B1-16}.
 */
public interface OrgAccountService {

    /**
     * Cuentas de entidad por estado, pagina bien.
     *
     * <p>{@code PENDING} (o sin estado) muestra las dos pendientes: la que aún
     * no verificó el correo y la que ya lo hizo y espera aprobación. Cualquier
     * otro valor debe ser un {@code UserStatus}.
     */
    Page<OrgAccountRow> list(String status, Pageable pageable);

    /**
     * Aprueba la cuenta y la entidad.
     *
     * <p>La aprueba ya esté verificada o no (decisión del administrador: quien
     * verifica la entidad por teléfono no necesita el correo). Deja la entidad
     * {@code ACTIVE} si no lo estuviera.
     */
    OrgAccountRow approve(Long userId);

    /**
     * Rechaza la cuenta.
     *
     * <p>La entidad baja a {@code REJECTED} solo si era {@code PENDING} y se
     * queda sin ninguna cuenta {@code ACTIVE}; si ya era {@code ACTIVE} (segunda
     * cuenta), se queda: el rechazo de un contacto no tumba la entidad.
     */
    OrgAccountRow reject(Long userId);
}