package com.verisure.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.proposal.CreateProposalRequest;
import com.verisure.backend.dto.proposal.ProposalRow;
import com.verisure.backend.service.ProposalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * El formulario público de la landing: una organización propone sin tener cuenta.
 *
 * <p>La ruta es pública en {@code SpringConfig}. La administradora decide después
 * desde {@code /api/admin/proposals}.
 */
@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    /** Crea una propuesta sin entidad detrás. Responde 201. */
    @PostMapping
    public ResponseEntity<ProposalRow> create(@Valid @RequestBody CreateProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proposalService.create(request));
    }
}
