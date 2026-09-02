package com.verisure.backend.seeder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.Proposal;
import com.verisure.backend.entity.enums.ProposalStatus;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.PartnerRepository;
import com.verisure.backend.repository.ProposalRepository;

import lombok.RequiredArgsConstructor;

/**
 * Siembra 6 propuestas, dos por cada {@code ProposalStatus}.
 *
 * <p>La primera lleva {@code partner = null} a propósito: {@code POST /api/proposals}
 * es un endpoint público, así que una organización sin cuenta puede proponer. Es
 * el único dato que prueba ese camino.
 *
 * <p>Las dos {@code ACCEPTED} apuntan a actividades <b>distintas</b>, porque la
 * relación con {@code Activity} es {@code @OneToOne}.
 *
 * <p>{@code scope} es el número de <b>personas beneficiarias</b> que alcanzaría la
 * propuesta, no un ámbito geográfico. Va en paralelo a {@code estimatedVolunteers},
 * que cuenta a quienes participan.
 *
 * <p>Dueña: BE2 · Tarea: B2-06
 */
@Component
@Order(4)
@Profile("!prod")
@RequiredArgsConstructor
public class ProposalSeeder implements CommandLineRunner {

    private final ProposalRepository proposalRepository;
    private final PartnerRepository partnerRepository;
    private final ActivityRepository activityRepository;

    @Override
    public void run(String... args) {
        if (proposalRepository.count() > 0) {
            return; // idempotente: no duplica al reiniciar
        }

        List<Partner> orgs = partnerRepository.findAll();
        Activity limpiezaDePlayas = buscarActividad("Limpieza de playas");
        Activity bancoDeAlimentos = buscarActividad("Reparto del banco de alimentos");

        proposalRepository.saveAll(List.of(
                // Propuesta pública: la envía una organización que todavía no tiene cuenta.
                prop("Acompañamiento telefónico semanal a personas mayores que viven solas.",
                        12, "desoledad", 60,
                        ProposalStatus.NEW, null, null, 5),

                prop("Apoyo escolar en centros de acogida durante el curso.",
                        20, "educar", 120,
                        ProposalStatus.NEW, orgs.get(6), null, 8),

                prop("Jornada de recogida de residuos en las playas del litoral.",
                        25, "voluntariado", 300,
                        ProposalStatus.ACCEPTED, orgs.get(5), limpiezaDePlayas, 12),

                prop("Clasificación y reparto de alimentos a familias vulnerables.",
                        15, "voluntariado", 450,
                        ProposalStatus.ACCEPTED, orgs.get(4), bancoDeAlimentos, 15),

                prop("Programa de mediación entre iguales en institutos.",
                        10, "acoso", 200,
                        ProposalStatus.REJECTED, orgs.get(7), null, 18),

                prop("Formación a familias sobre detección temprana del acoso.",
                        8, "acoso", 90,
                        ProposalStatus.REJECTED, orgs.get(3), null, 22)));
    }

    private Proposal prop(String description, int estimatedVolunteers, String suggestedLine,
                          Integer scope, ProposalStatus status, Partner partner,
                          Activity activity, int diaDeEnero) {
        Proposal p = new Proposal();
        p.setDescription(description);
        p.setEstimatedVolunteers(estimatedVolunteers);
        p.setSuggestedLine(suggestedLine);
        p.setScope(scope);
        p.setStatus(status);
        p.setPartner(partner);
        p.setActivity(activity);
        p.setConsentAt(instante(diaDeEnero));
        p.setCreatedAt(instante(diaDeEnero));
        return p;
    }

    private Activity buscarActividad(String titulo) {
        return activityRepository.findAll().stream()
                .filter(a -> a.getTitle().equals(titulo))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "ActivitySeeder no dejó sembrada la actividad: " + titulo));
    }

    private Instant instante(int diaDeEnero) {
        return LocalDate.of(2026, 1, diaDeEnero).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
