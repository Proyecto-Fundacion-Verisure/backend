package com.verisure.backend.seeder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
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
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.ParticipationClosureRepository;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.service.CertificateReference;

import lombok.RequiredArgsConstructor;

/**
 * Siembra los <b>dos</b> closures: 7 de participación y 3 de actividad.
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

    /**
     * PNG de un píxel, válido, para que las evidencias sembradas existan en disco.
     *
     * <p>Sin archivo detrás, el enlace de la evidencia da 404 en la demo. La
     * carpeta {@code uploads/} está en {@code .gitignore}, así que el seeder lo
     * escribe él mismo al sembrar.
     */
    private static final byte[] PLACEHOLDER_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGN4xmADAALyASPcgvPVAAAAAElFTkSuQmCC");

    private final ParticipationClosureRepository participationClosureRepository;
    private final ActivityClosureRepository activityClosureRepository;
    private final RegistrationRepository registrationRepository;
    private final ActivityRepository activityRepository;

    @Override
    public void run(String... args) {
        if (participationClosureRepository.count() > 0 || activityClosureRepository.count() > 0) {
            return; // idempotente: no duplica al reiniciar
        }

        List<Registration> elderlyClosed = byActivityAndStatus(
                "Acompañamiento a mayores", RegistrationStatus.CLOSED);
        List<Registration> bullyingClosed = byActivityAndStatus(
                "Campaña contra el acoso escolar", RegistrationStatus.CLOSED);
        List<Registration> pending = byStatus(RegistrationStatus.PENDING_CLOSURE);

        List<ParticipationClosure> closures = new ArrayList<>();

        // Las tres de «Acompañamiento a mayores», previsto 20 h.
        closures.addAll(participationClosures(elderlyClosed,
                new int[] {20, 22, 18}, new int[] {5, 4, 3},
                "Experiencia muy positiva; repetiría el próximo trimestre.",
                "acompanamiento", LocalDate.of(2026, 3, 30)));

        // Las tres de «Campaña contra el acoso escolar», previsto 10 h.
        closures.addAll(participationClosures(bullyingClosed,
                new int[] {10, 12, 9}, new int[] {4, 5, 4},
                "Los institutos pidieron repetir la sesión con las familias.",
                "campana-acoso", LocalDate.of(2026, 5, 25)));

        // Una sola de las dos pending: la administradora ve que aún falta gente.
        if (!pending.isEmpty()) {
            closures.add(participationClosure(pending.get(0), 11, 4,
                    "El taller se quedó algo corto de tiempo.",
                    null, LocalDate.of(2026, 4, 27)));
        }

        // La referencia es CERT-año-id: necesita el id que asigna la base de
        // datos, por eso se rellena tras el save y no en el constructor-ayudante (B1-21).
        closures = participationClosureRepository.saveAll(closures);
        closures.forEach(pc -> pc.setReference(CertificateReference.forClosure(pc)));
        participationClosureRepository.saveAll(closures);

        List<ActivityClosure> activityClosures = new ArrayList<>();
        if (!elderlyClosed.isEmpty()) {
            activityClosures.add(activityClosure(elderlyClosed.get(0).getActivity(),
                    ActivityClosureStatus.CLOSED, 4,
                    "Colaboración muy sólida. Las horas reportadas superan lo previsto.",
                    "Conviene cerrar el grupo de voluntariado dos semanas antes de empezar.",
                    LocalDate.of(2026, 4, 3)));
        }
        if (!bullyingClosed.isEmpty()) {
            activityClosures.add(activityClosure(bullyingClosed.get(0).getActivity(),
                    ActivityClosureStatus.CLOSED, 5,
                    "Muy buena acogida en los tres institutos.",
                    "Programar las sesiones fuera del periodo de exámenes.",
                    LocalDate.of(2026, 5, 29)));
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

    /**
     * Un cierre por inscripción, con horas y valoración escalonadas.
     *
     * <p>Solo las posiciones pares llevan evidencia, para que el recuento de
     * evidencias de la pantalla de cierre no sea trivial.
     */
    private List<ParticipationClosure> participationClosures(List<Registration> registrations,
                                                             int[] hours, int[] ratings, String comment,
                                                             String evidencePrefix, LocalDate submittedOn) {
        List<ParticipationClosure> closures = new ArrayList<>();
        for (int i = 0; i < registrations.size(); i++) {
            String evidenceUrl = i % 2 == 0
                    ? ensureEvidenceFile("/uploads/evidencias/" + evidencePrefix + "-" + (i + 1) + ".png")
                    : null;
            closures.add(participationClosure(registrations.get(i),
                    hours[i % hours.length], ratings[i % ratings.length],
                    comment, evidenceUrl, submittedOn));
        }
        return closures;
    }

    /** Escribe el PNG de relleno en la ruta si no está; devuelve la misma ruta. No sobrescribe. */
    private String ensureEvidenceFile(String relativeUrl) {
        Path target = Paths.get("." + relativeUrl);
        try {
            Files.createDirectories(target.getParent());
            if (Files.notExists(target)) {
                Files.write(target, PLACEHOLDER_PNG);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo escribir la evidencia de la semilla " + target, e);
        }
        return relativeUrl;
    }

    private List<Registration> byStatus(RegistrationStatus status) {
        return registrationRepository.findAll().stream()
                .filter(r -> r.getStatus() == status)
                .toList();
    }

    /**
     * Se compara por id y no por título: {@code Registration.activity} es
     * {@code LAZY} y el seeder corre sin transacción, así que leer el título del
     * proxy fallaría; el id sí está disponible sin inicializarlo.
     */
    private List<Registration> byActivityAndStatus(String activityTitle, RegistrationStatus status) {
        Long activityId = activityIdByTitle(activityTitle);
        return byStatus(status).stream()
                .filter(r -> r.getActivity().getId().equals(activityId))
                .toList();
    }

    private Long activityIdByTitle(String title) {
        return activityRepository.findAll().stream()
                .filter(a -> a.getTitle().equals(title))
                .map(Activity::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "ActivitySeeder no dejó sembrada la actividad: " + title));
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
