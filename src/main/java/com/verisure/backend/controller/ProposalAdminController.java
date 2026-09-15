package com.verisure.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.activity.ActivityResponse;
import com.verisure.backend.dto.proposal.ProposalDetailResponse;
import com.verisure.backend.dto.proposal.ProposalRow;
import com.verisure.backend.entity.enums.ProposalStatus;
import com.verisure.backend.service.ProposalService;

import lombok.RequiredArgsConstructor;

/**
 * La bandeja de propuestas. Solo administración.
 *
 * <p>No lleva {@code @PreAuthorize}: la cadena de seguridad ya reserva
 * {@code /api/admin/**} al rol {@code ADMIN}.
 *
 * <p>El formulario público que crea propuestas, {@code POST /api/proposals}, es
 * otra tarea y no cuelga de aquí.
 *
 * <p>Dueña: BE3 · Tarea: B2-07.
 */
@RestController
@RequestMapping("/api/admin/proposals")
@RequiredArgsConstructor
public class ProposalAdminController {

    private final ProposalService proposalService;

    /** Una página de la bandeja, filtrada por estado si se pide. */
    @GetMapping
    public Page<ProposalRow> list(
            @RequestParam(required = false) ProposalStatus status,
            Pageable pageable) {

        return proposalService.list(status, pageable);
    }

    /** La ficha de una propuesta. */
    @GetMapping("/{id}")
    public ProposalDetailResponse detail(@PathVariable Long id) {
        return proposalService.detail(id);
    }

    /**
     * Acepta la propuesta y devuelve la actividad que sale de ella.
     *
     * <p>Responde 201 y no 200 porque crea una actividad nueva.
     */
    @PostMapping("/{id}/accept")
    public ResponseEntity<ActivityResponse> accept(@PathVariable Long id) {
        ActivityResponse body = proposalService.accept(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** Rechaza la propuesta. */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        proposalService.reject(id);
        return ResponseEntity.noContent().build();
    }
}
