package com.verisure.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.verisure.backend.dto.proposal.CreateProposalRequest;
import com.verisure.backend.dto.proposal.ProposalDetailResponse;
import com.verisure.backend.dto.proposal.ProposalRow;
import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.Proposal;
import com.verisure.backend.entity.enums.ProposalStatus;
import com.verisure.backend.mapper.ActivityMapper;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.ProposalRepository;

@ExtendWith(MockitoExtension.class)
class ProposalServiceImplTest {

    @Mock ProposalRepository proposalRepository;
    @Mock ActivityRepository activityRepository;
    @Mock ActivityMapper activityMapper;

    @InjectMocks ProposalServiceImpl service;

    private static CreateProposalRequest request(String line, Integer volunteers) {
        return new CreateProposalRequest(
                "  Fundación Ejemplo ", "g12345678", " María García ", " maria@ejemplo.ex ", " 600123456 ",
                volunteers, line, " Acompañamiento a personas mayores. ", true);
    }

    private static Proposal publicProposal() {
        Proposal proposal = new Proposal();
        proposal.setId(5L);
        proposal.setDescription("Acompañamiento");
        proposal.setEstimatedVolunteers(10);
        proposal.setStatus(ProposalStatus.NEW);
        proposal.setConsentAt(Instant.now());
        proposal.setCreatedAt(Instant.now());
        proposal.setApplicantName("Fundación Ejemplo");
        proposal.setApplicantCif("G12345678");
        proposal.setContactName("María García");
        proposal.setContactEmail("maria@ejemplo.ex");
        proposal.setContactPhone("600123456");
        return proposal;
    }

    @Test
    void create_savesNewProposalWithApplicantDataAndConsent() {
        when(proposalRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        ProposalRow row = service.create(request("desoledad", 10));

        ArgumentCaptor<Proposal> saved = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(saved.capture());
        Proposal proposal = saved.getValue();
        assertEquals("Fundación Ejemplo", proposal.getApplicantName());
        assertEquals("G12345678", proposal.getApplicantCif());
        assertEquals("María García", proposal.getContactName());
        assertEquals("maria@ejemplo.ex", proposal.getContactEmail());
        assertEquals("600123456", proposal.getContactPhone());
        assertEquals("Acompañamiento a personas mayores.", proposal.getDescription());
        assertEquals("desoledad", proposal.getSuggestedLine());
        assertEquals(10, proposal.getEstimatedVolunteers());
        assertEquals(ProposalStatus.NEW, proposal.getStatus());
        assertNotNull(proposal.getConsentAt());
        assertNull(proposal.getPartner());
        assertEquals("Fundación Ejemplo", row.partnerName());
    }

    @Test
    void create_withoutLineOrVolunteers_keepsThemEmpty() {
        when(proposalRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        ProposalRow row = service.create(request("  ", null));

        assertNull(row.suggestedLine());
        assertEquals(0, row.estimatedVolunteers());
    }

    @Test
    void detail_withoutPartner_usesTheApplicantData() {
        when(proposalRepository.findByIdWithPartner(5L)).thenReturn(Optional.of(publicProposal()));

        ProposalDetailResponse detail = service.detail(5L);

        assertEquals("Fundación Ejemplo", detail.partnerName());
        assertEquals("G12345678", detail.cif());
        assertEquals("María García", detail.contactName());
        assertEquals("maria@ejemplo.ex", detail.email());
        assertEquals("600123456", detail.phone());
    }

    @Test
    void detail_withPartner_prefersThePartnerData() {
        Partner partner = new Partner();
        partner.setName("Cáritas");
        partner.setCif("R0800000A");
        partner.setContactName("Marta");
        partner.setEmail("marta@caritas.ex");
        partner.setPhone("900000000");
        Proposal proposal = publicProposal();
        proposal.setPartner(partner);
        when(proposalRepository.findByIdWithPartner(5L)).thenReturn(Optional.of(proposal));

        ProposalDetailResponse detail = service.detail(5L);

        assertEquals("Cáritas", detail.partnerName());
        assertEquals("R0800000A", detail.cif());
        assertEquals("marta@caritas.ex", detail.email());
    }

    @Test
    void accept_withoutPartner_titlesTheDraftWithTheApplicantName() {
        when(proposalRepository.findByIdWithPartner(5L)).thenReturn(Optional.of(publicProposal()));
        when(activityRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        service.accept(5L);

        ArgumentCaptor<Activity> saved = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(saved.capture());
        assertEquals("Propuesta de Fundación Ejemplo", saved.getValue().getTitle());
    }
}
