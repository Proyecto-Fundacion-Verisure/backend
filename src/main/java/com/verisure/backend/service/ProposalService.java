package com.verisure.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.verisure.backend.dto.activity.ActivityResponse;
import com.verisure.backend.dto.proposal.ProposalDetailResponse;
import com.verisure.backend.dto.proposal.ProposalRow;
import com.verisure.backend.entity.enums.ProposalStatus;

/**
 * La bandeja de propuestas de la administradora.
 *
 * <p>Una propuesta nace {@code NEW} y ahí se queda hasta que se decide: no hay
 * estado «leída», porque abrirla no aporta información que nadie vaya a usar.
 *
 * <p>Dueña: BE3 · Tarea: B2-07.
 */
public interface ProposalService {

    /** Una página de la bandeja. El estado es opcional. */
    Page<ProposalRow> list(ProposalStatus status, Pageable pageable);

    /** La ficha, con la descripción y el contacto de quien la envió. */
    ProposalDetailResponse detail(Long proposalId);

    /**
     * Acepta la propuesta y crea con ella una actividad en borrador.
     *
     * <p>Una propuesta ya decidida —aceptada o rechazada— responde
     * {@code PROPOSAL_ALREADY_DECIDED}.
     */
    ActivityResponse accept(Long proposalId);

    /**
     * Rechaza la propuesta.
     *
     * <p>Sin motivo y sin cuerpo, como el rechazo de una inscripción: es un
     * estado, no una conversación.
     */
    void reject(Long proposalId);
}
