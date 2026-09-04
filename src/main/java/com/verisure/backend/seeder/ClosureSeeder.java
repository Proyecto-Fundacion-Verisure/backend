package com.verisure.backend.seeder;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.ActivityClosure;
import com.verisure.backend.entity.ParticipationClosure;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.enums.ActivityClosureStatus;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.repository.ActivityClosureRepository;
import com.verisure.backend.repository.ParticipationClosureRepository;
import com.verisure.backend.repository.RegistrationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Siembra los <b>dos</b> closures: 4 de participación y 2 de actividad.
 *
 * <p>Las horas declaradas se separan a propósito de las previstas —una por
 * encima y otra por debajo— porque lo que la pantalla de participationClosure tiene que
 * enseñar es justamente el contraste entre previsto y reportado. Si todas
 * cuadraran, esa pantalla no demostraría nada.
 *
 * <p><b>Cruce entre dominios.</b> Este seeder es de BE1 pero depende por
 * completo de que {@link RegistrationSeeder} haya dejado inscripciones en
 * {@code CLOSED} y {@code PENDING_CLOSURE}. Sin ellas no hay nada que cerrar y
 * el dashboard sale vacío en la demo, sin ningún error que lo avise.
 *
 * <p>Dueña: BE1 · Tareas: B1-03 (participación) y B1-04 (actividad)
 */
@Component
@Order(7)
@Profile("!prod")
@RequiredArgsConstructor
public class ClosureSeeder implements CommandLineRunner {

    private final ParticipationClosureRepository participationClosureRepository;
    private final ActivityClosureRepository activityClosureRepository;
    private final RegistrationRepository registrationRepository;

    @Override
    public void run(String... args) {
        if (participationClosureRepository.count() > 0 || activityClosureRepository.count() > 0) {
            return; // idempotente: no duplica al reiniciar
        }

        List<Registration> closed = byStatus(RegistrationStatus.CLOSED);
        List<Registration> pending = byStatus(RegistrationStatus.PENDING_CLOSURE);

        List<ParticipationClosure> closures = new ArrayList<>();

        // Las tres de «Acompañamiento a mayores», previsto 20 h.
        int[] hours   = {20, 22, 18};
        int[] ratings = {5, 4, 3};
        for (int i = 0; i < closed.size(); i++) {
            closures.add(participationClosure(closed.get(i),
                    hours[i % hours.length],
                    ratings[i % ratings.length],
                    "Experiencia muy positiva; repetiría el próximo trimestre.",
                    // Solo algunos suben evidencia, para que el recuento no sea trivial.
                    i % 2 == 0 ? "/uploads/evidencias/acompanamiento-" + (i + 1) + ".pdf" : null,
                    LocalDate.of(2026, 3, 30)));
        }

        // Una sola de las dos pending: la administradora ve que aún falta gente.
        if (!pending.isEmpty()) {
            closures.add(participationClosure(pending.get(0), 11, 4,
                    "El taller se quedó algo corto de tiempo.",
                    null, LocalDate.of(2026, 4, 27)));
        }

        participationClosureRepository.saveAll(closures);

        List<ActivityClosure> activityClosures = new ArrayList<>();
        if (!closed.isEmpty()) {
            activityClosures.add(activityClosure(closed.get(0).getActivity(),
                    ActivityClosureStatus.CLOSED, 4,
                    "Colaboración muy sólida. Las horas reportadas superan lo previsto.",
                    "Conviene cerrar el grupo de voluntariado dos semanas antes de empezar.",
                    LocalDate.of(2026, 4, 3)));
        }
        if (!pending.isEmpty()) {
            // A medio rellenar: es la pantalla principal de B1-04.
            activityClosures.add(activityClosure(pending.get(0).getActivity(),
                    ActivityClosureStatus.DRAFT, 3,
                    "Pendiente de que cierren el resto de participantes.",
                    null, null));
        }
        activityClosureRepository.saveAll(activityClosures);
    }

    private List<Registration> byStatus(RegistrationStatus status) {
        return registrationRepository.findAll().stream()
                .filter(r -> r.getStatus() == status)
                .toList();
    }

    private ParticipationClosure participationClosure(Registration registration, int actualHours, int rating,
                                        String comment, String evidenceUrl, LocalDate submittedOn) {
        ParticipationClosure pc = new ParticipationClosure();
        pc.setRegistration(registration);
        pc.setActualHours(actualHours);
        pc.setRating(rating);
        pc.setComment(comment);
        pc.setEvidenceUrl(evidenceUrl);
        pc.setSubmittedAt(submittedOn.atStartOfDay().toInstant(ZoneOffset.UTC));
        return pc;
    }

    private ActivityClosure activityClosure(Activity activity, ActivityClosureStatus status,
                                              Integer collaborationRating, String closingNotes,
                                              String lessonsLearned, LocalDate closedOn) {
        ActivityClosure ac = new ActivityClosure();
        ac.setActivity(activity);
        ac.setStatus(status);
        ac.setCollaborationRating(collaborationRating);
        ac.setClosingNotes(closingNotes);
        ac.setLessonsLearned(lessonsLearned);
        ac.setClosedAt(closedOn == null ? null
                : closedOn.atStartOfDay().toInstant(ZoneOffset.UTC));
        return ac;
    }
}
