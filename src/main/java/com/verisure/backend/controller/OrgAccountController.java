package com.verisure.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.orgaccount.OrgAccountRow;
import com.verisure.backend.service.NotificationService;
import com.verisure.backend.service.OrgAccountService;

import lombok.RequiredArgsConstructor;

/**
 * Cuentas de entidad para administración · {@code B1-16}.
 *
 * <p>Toda la clase cae detrás de {@code /api/admin/**}, que la cadena de
 * seguridad ya restringe a {@code ADMIN}: aquí no hay comprobación de rol.
 * Los avisos de aprobación y rechazo se mandan desde el controlador, fuera de
 * la transacción del servicio.
 */
@RestController
@RequestMapping("/api/admin/org-accounts")
@RequiredArgsConstructor
public class OrgAccountController {

    private final OrgAccountService orgAccountService;
    private final NotificationService notificationService;

    /**
     * {@code GET /api/admin/org-accounts}. Relación de cuentas de entidad.
     *
     * <p>{@code status} filtra por estado; con {@code PENDING} se ven las dos
     * pendientes (sin verificar y pendientes de aprobar) · {@code B1-16}.
     */
    @GetMapping
    public Page<OrgAccountRow> list(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return orgAccountService.list(status, pageable);
    }

    /**
     * {@code PATCH /api/admin/org-accounts/{id}/approve}. Aprueba la cuenta y,
     * si hace falta, la entidad · {@code B1-16}.
     */
    @PatchMapping("/{userId}/approve")
    public OrgAccountRow approve(@PathVariable Long userId) {
        OrgAccountRow row = orgAccountService.approve(userId);
        notificationService.notifyOrgAccountApproved(userId);
        return row;
    }

    /**
     * {@code PATCH /api/admin/org-accounts/{id}/reject}. Rechaza la cuenta;
     * la entidad solo cae si era nueva y no queda nadie activo · {@code B1-16}.
     */
    @PatchMapping("/{userId}/reject")
    public OrgAccountRow reject(@PathVariable Long userId) {
        OrgAccountRow row = orgAccountService.reject(userId);
        notificationService.notifyOrgAccountRejected(userId);
        return row;
    }
}