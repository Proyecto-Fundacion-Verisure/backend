package com.verisure.backend.service;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.activity.ActivityResponse;
import com.verisure.backend.dto.proposal.CreateProposalRequest;
import com.verisure.backend.dto.proposal.ProposalDetailResponse;
import com.verisure.backend.dto.proposal.ProposalRow;
import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.Proposal;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.entity.enums.ProposalStatus;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.mapper.ActivityMapper;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.ProposalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProposalServiceImpl implements ProposalService {

    // Marcadores a la vista: la propuesta no trae línea, modalidad ni horas, y en
    // Activity esos campos no admiten nulo.
    private static final String UNDEFINED_LINE = "sin definir";
    private static final String DEFAULT_MODE = "PRESENCIAL";
    private static final int PLACEHOLDER_HOURS = 1;

    /** Longitud de la columna {@code title} de {@code Activity}. */
    private static final int TITLE_MAX_LENGTH = 160;

    private final ProposalRepository proposalRepository;
    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProposalRow create(CreateProposalRequest request) {
        Instant now = Instant.now();
        String line = request.line() != null ? request.line().trim() : "";

        Proposal proposal = new Proposal();
        proposal.setApplicantName(request.organizationName().trim());
        proposal.setApplicantCif(request.cif().trim().toUpperCase());
        proposal.setContactName(request.contactName().trim());
        proposal.setContactEmail(request.email().trim());
        proposal.setContactPhone(request.phone().trim());
        proposal.setDescription(request.description().trim());
        proposal.setSuggestedLine(line.isEmpty() ? null : line);
        // Opcional en el formulario, pero la columna no admite nulo: 0 es «sin indicar».
        proposal.setEstimatedVolunteers(request.estimatedVolunteers() != null ? request.estimatedVolunteers() : 0);
        proposal.setStatus(ProposalStatus.NEW);
        proposal.setConsentAt(now);
        proposal.setCreatedAt(now);

        Proposal saved = proposalRepository.save(proposal);
        return new ProposalRow(
                saved.getId(),
                saved.getApplicantName(),
                saved.getSuggestedLine(),
                saved.getEstimatedVolunteers(),
                saved.getScope(),
                saved.getStatus(),
                saved.getCreatedAt(),
                null);
    }

    /** {@inheritDoc} */
    @Override
    public Page<ProposalRow> list(ProposalStatus status, Pageable pageable) {
        return proposalRepository.findRows(status, pageable);
    }

    /** {@inheritDoc} */
    @Override
    public ProposalDetailResponse detail(Long proposalId) {
        return toDetail(findOrFail(proposalId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ActivityResponse accept(Long proposalId) {
        Proposal proposal = findOrFail(proposalId);
        requireUndecided(proposal);

        Activity activity = activityRepository.save(toDraftActivity(proposal));

        proposal.setActivity(activity);
        proposal.setStatus(ProposalStatus.ACCEPTED);

        return activityMapper.toResponse(activity);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void reject(Long proposalId) {
        Proposal proposal = findOrFail(proposalId);
        requireUndecided(proposal);

        proposal.setStatus(ProposalStatus.REJECTED);
    }

    /**
     * La ficha, con los datos de la entidad resueltos aparte.
     *
     * <p>Una propuesta pública no tiene entidad detrás: el nombre, el CIF y el
     * contacto son los que escribió quien la envió.
     */
    private ProposalDetailResponse toDetail(Proposal proposal) {
        Partner partner = proposal.getPartner();
        String partnerName = partner != null ? partner.getName() : proposal.getApplicantName();
        String cif = partner != null ? partner.getCif() : proposal.getApplicantCif();
        String contactName = partner != null ? partner.getContactName() : proposal.getContactName();
        String email = partner != null ? partner.getEmail() : proposal.getContactEmail();
        String phone = partner != null ? partner.getPhone() : proposal.getContactPhone();

        Activity activity = proposal.getActivity();
        Long activityId = activity != null ? activity.getId() : null;

        return new ProposalDetailResponse(
                proposal.getId(),
                partnerName,
                proposal.getSuggestedLine(),
                proposal.getEstimatedVolunteers(),
                proposal.getScope(),
                proposal.getStatus(),
                proposal.getCreatedAt(),
                activityId,
                proposal.getDescription(),
                contactName,
                email,
                phone,
                proposal.getConsentAt(),
                cif);
    }

    /**
     * La actividad que sale de una propuesta, en borrador.
     *
     * <p>Solo la descripción, la línea, las plazas y la entidad vienen de la
     * propuesta; el resto son marcadores que la administradora sustituye antes de
     * publicar, y que nadie ve porque {@code DRAFT} no sale en ningún catálogo.
     */
    private Activity toDraftActivity(Proposal proposal) {
        Partner partner = proposal.getPartner();
        LocalDate today = LocalDate.now();

        String suggestedLine = proposal.getSuggestedLine();
        String line = suggestedLine != null ? suggestedLine : UNDEFINED_LINE;

        Activity activity = new Activity();
        activity.setTitle(titleFor(partner != null ? partner.getName() : proposal.getApplicantName()));
        activity.setDescription(proposal.getDescription());
        activity.setLine(line);
        activity.setMode(DEFAULT_MODE);
        activity.setStartDate(today);
        activity.setEndDate(today);
        activity.setRegistrationDeadline(today);
        activity.setHours(PLACEHOLDER_HOURS);
        activity.setSpots(proposal.getEstimatedVolunteers());
        activity.setStatus(ActivityStatus.DRAFT);
        activity.setPartner(partner);

        return activity;
    }

    /** El título provisional dice de quién es la propuesta, que es lo único que se sabe. */
    private String titleFor(String proposerName) {
        if (proposerName == null) {
            return "Propuesta sin entidad";
        }
        String title = "Propuesta de " + proposerName;

        // El nombre de la entidad admite 150 caracteres, así que sin recorte un
        // nombre largo tumba el insert.
        boolean tooLong = title.length() > TITLE_MAX_LENGTH;
        if (tooLong) {
            return title.substring(0, TITLE_MAX_LENGTH);
        }
        return title;
    }

    private void requireUndecided(Proposal proposal) {
        boolean alreadyDecided = proposal.getStatus() != ProposalStatus.NEW;
        if (alreadyDecided) {
            throw new DomainException(ErrorCode.PROPOSAL_ALREADY_DECIDED);
        }
    }

    private Proposal findOrFail(Long proposalId) {
        return proposalRepository.findByIdWithPartner(proposalId)
                .orElseThrow(() -> NotFoundException.of("propuesta", proposalId));
    }
}
