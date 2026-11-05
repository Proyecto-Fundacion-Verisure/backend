package com.verisure.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.verisure.backend.dto.proposal.ProposalRow;
import com.verisure.backend.entity.enums.ProposalStatus;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.service.ProposalService;
import com.verisure.backend.support.ControllerTest;

/** {@code POST /api/proposals} sin token, con la cadena de seguridad real. */
@ControllerTest(ProposalController.class)
class ProposalControllerIT {

    private static final String VALID = """
            {
              "organizationName": "Fundación Ejemplo",
              "cif": "G12345678",
              "contactName": "María García",
              "email": "maria@ejemplo.ex",
              "phone": "600123456",
              "estimatedVolunteers": 10,
              "line": "desoledad",
              "description": "Acompañamiento a personas mayores.",
              "consent": true
            }
            """;

    @Autowired MockMvc mockMvc;

    @MockitoBean UserRepository userRepository;
    @MockitoBean ProposalService proposalService;

    private ResultActions send(String body) throws Exception {
        return mockMvc.perform(post("/api/proposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void create_withoutToken_returns201() throws Exception {
        when(proposalService.create(any())).thenReturn(new ProposalRow(
                7L, "Fundación Ejemplo", "desoledad", 10, null, ProposalStatus.NEW, Instant.now(), null));

        send(VALID)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void create_withoutConsent_returns400AndDoesNotSave() throws Exception {
        send(VALID.replace("\"consent\": true", "\"consent\": false"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.consent[0]").value("Debes aceptar la política de privacidad."));

        verify(proposalService, never()).create(any());
    }

    @Test
    void create_withInvalidCifAndEmail_reportsEachField() throws Exception {
        send(VALID.replace("G12345678", "123").replace("maria@ejemplo.ex", "no-es-correo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.cif[0]").value("Introduce un CIF válido."))
                .andExpect(jsonPath("$.fields.email[0]").value("Introduce un correo válido."));

        verify(proposalService, never()).create(any());
    }

    @Test
    void create_withBlankRequiredFields_returns400() throws Exception {
        send("{\"consent\": true}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.organizationName").isArray())
                .andExpect(jsonPath("$.fields.contactName").isArray())
                .andExpect(jsonPath("$.fields.description").isArray());

        verify(proposalService, never()).create(any());
    }

    @Test
    void create_withoutLineOrVolunteers_isAccepted() throws Exception {
        when(proposalService.create(any())).thenReturn(new ProposalRow(
                8L, "Fundación Ejemplo", null, 0, null, ProposalStatus.NEW, Instant.now(), null));

        send(VALID.replace("\"estimatedVolunteers\": 10,", "").replace("\"line\": \"desoledad\",", ""))
                .andExpect(status().isCreated());
    }
}
