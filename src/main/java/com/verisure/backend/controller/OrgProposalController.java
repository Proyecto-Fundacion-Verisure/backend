package com.verisure.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.org.CreateOrgProposalRequest;
import com.verisure.backend.dto.org.OrgProposalRow;
import com.verisure.backend.service.OrgProposalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Las propuestas del rol entidad · {@code B2-16}.
 *
 * <p>No lleva {@code @PreAuthorize}: la cadena de seguridad ya reserva
 * {@code /api/org/**} al rol {@code PARTNER}.
 *
 * <p>Va aparte de {@link ProposalAdminController}, que es la bandeja de la
 * Fundación: son dos vistas distintas de la misma tabla.
 *
 * <p>Dueña: BE3 · Tarea: B2-16.
 */
@RestController
@RequestMapping("/api/org/proposals")
@RequiredArgsConstructor
public class OrgProposalController {

    private final OrgProposalService orgProposalService;

    /** Una página de las propuestas que ha escrito la entidad, de la más reciente a la más vieja. */
    @GetMapping
    public Page<OrgProposalRow> list(Pageable pageable, Authentication authentication) {
        return orgProposalService.list(pageable, authentication.getName());
    }

    /** Crea una propuesta a nombre de la entidad. Responde 201. */
    @PostMapping
    public ResponseEntity<OrgProposalRow> create(
            @Valid @RequestBody CreateOrgProposalRequest request,
            Authentication authentication) {

        OrgProposalRow body = orgProposalService.create(request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
