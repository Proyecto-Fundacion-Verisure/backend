package com.verisure.backend.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.org.CreateOrgProposalRequest;
import com.verisure.backend.dto.org.OrgProposalRow;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.Proposal;
import com.verisure.backend.entity.enums.ProposalStatus;
import com.verisure.backend.repository.ProposalRepository;

import lombok.RequiredArgsConstructor;

/**
 * Proponer desde dentro · {@code B2-16}.
 *
 * <p>Dueña: BE3 · Tarea: B2-16.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgProposalServiceImpl implements OrgProposalService {

    private final ProposalRepository proposalRepository;
    private final PartnerResolver partnerResolver;

    /** {@inheritDoc} */
    @Override
    public Page<OrgProposalRow> list(Pageable pageable, String userEmail) {
        Long partnerId = partnerResolver.resolve(userEmail).getId();
        return proposalRepository.findOrgRows(partnerId, pageable);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public OrgProposalRow create(CreateOrgProposalRequest request, String userEmail) {
        Partner partner = partnerResolver.resolve(userEmail);
        Instant now = Instant.now();

        Proposal proposal = new Proposal();
        proposal.setDescription(request.description());
        proposal.setSuggestedLine(request.suggestedLine());
        proposal.setEstimatedVolunteers(request.estimatedVolunteers());
        proposal.setScope(request.scope());
        proposal.setPartner(partner);
        proposal.setStatus(ProposalStatus.NEW);
        proposal.setCreatedAt(now);
        // El consentimiento no se vuelve a pedir: la entidad lo dio al registrarse
        // y la columna no admite nulo. La casilla solo tiene sentido en el
        // formulario público, donde quien propone no tiene cuenta.
        proposal.setConsentAt(now);

        return toRow(proposalRepository.save(proposal));
    }

    /** Una propuesta recién creada nunca tiene actividad: solo la tienen las aceptadas. */
    private OrgProposalRow toRow(Proposal proposal) {
        return new OrgProposalRow(
                proposal.getId(),
                proposal.getDescription(),
                proposal.getSuggestedLine(),
                proposal.getEstimatedVolunteers(),
                proposal.getScope(),
                proposal.getStatus(),
                proposal.getCreatedAt(),
                null);
    }
}
