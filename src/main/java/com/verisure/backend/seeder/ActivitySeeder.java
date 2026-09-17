package com.verisure.backend.seeder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.PartnerRepository;
import com.verisure.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Siembra las 13 actividades: al menos tres por cada línea de acción, los
 * <b>siete</b> {@code ActivityStatus} representados y una devuelta a borrador
 * con nota de revisión.
 *
 * <p>Los valores de {@code line} y {@code mode} no son opinables: frontend
 * filtra por ellos y los tiene fijados en {@code src/constants/activityLines.js}.
 * Las líneas van en minúsculas y los modos en mayúsculas.
 *
 * <p>Los aforos son pequeños (2 a 10) a propósito: con ocho empleadas sembradas,
 * una actividad de 20 plazas no se podría llenar nunca y su estado {@code FULL}
 * sería mentira.
 *
 * <p>Fechas mixtas: las pasadas son fijas de 2026, para que los agregados del
 * dashboard por año sean idénticos en las tres máquinas; las vivas son relativas
 * a today, para que siempre haya plazo de inscripción abierto.
 *
 * <p>Ese today es el del día en que se siembra, no el de la demo: el seeder no
 * vuelve a correr si ya hay filas. Por eso las abiertas llevan un mes de margen
 * en el plazo y en el inicio: el scheduler pasa a {@code IN_PROGRESS} todo lo que
 * ya ha empezado, y desde ahí nadie puede apuntarse.
 *
 * <p>Dueña: BE2 · Tarea: B2-01
 */
@Component
@Order(3)
@Profile("!prod")
@RequiredArgsConstructor
public class ActivitySeeder implements CommandLineRunner {

    private final ActivityRepository activityRepository;
    private final PartnerRepository partnerRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (activityRepository.count() > 0) {
            return; // idempotente: no duplica al reiniciar
        }

        List<Partner> partners = partnerRepository.findAll();
        User admin = firstUserWithRole(Role.ADMIN);
        // Las que proponen las entidades las crea la persona de esa misma entidad:
        // el listado del rol entidad filtra por su organización y el correo de
        // aprobación va a quien la creó.
        User caritasUser = partnerUserOf(partners.get(0));
        User redCrossUser = partnerUserOf(partners.get(5));

        LocalDate today = LocalDate.now();
        List<Activity> activities = new ArrayList<>();

        // ── Pasadas · fechas fijas de 2026 ──────────────────────────────────────
        // La 1 es la que sostiene el dashboard: sus inscripciones acaban en CLOSED.
        activities.add(activity("Acompañamiento a mayores",
                "Visitas semanales a personas mayores en situación de soledad no deseada.",
                "desoledad", "PRESENCIAL", "Barcelona", 8, 20,
                ActivityStatus.FINISHED, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 27),
                LocalDate.of(2026, 2, 20), partners.get(0), admin));

        activities.add(activity("Alfabetización digital",
                "Talleres para que personas mayores aprendan a usar el móvil y la banca en línea.",
                "desoledad", "MIXTO", "Madrid", 6, 12,
                ActivityStatus.FINISHED, LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 24),
                LocalDate.of(2026, 3, 25), partners.get(1), admin));

        activities.add(activity("Campaña contra el acoso escolar",
                "Sensibilización en centros educativos sobre el acoso entre iguales.",
                "acoso", "PRESENCIAL", "Barcelona", 5, 10,
                ActivityStatus.FINISHED, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 22),
                LocalDate.of(2026, 4, 22), partners.get(3), admin));

        activities.add(activity("Autoprotección para adolescentes",
                "Taller de autoprotección y uso seguro de redes sociales.",
                "acoso", "MIXTO", "Sevilla", 6, 10,
                ActivityStatus.CANCELLED, LocalDate.of(2026, 2, 9), LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 1, 30), partners.get(3), admin));

        // ── Vivas · fechas relativas a today ──────────────────────────────────────
        activities.add(activity("Refuerzo escolar",
                "Apoyo escolar para menores en riesgo de exclusión.",
                "educar", "PRESENCIAL", "Madrid", 6, 30,
                ActivityStatus.IN_PROGRESS, today.minusDays(10), today.plusDays(45),
                today.minusDays(20), partners.get(2), admin));

        activities.add(activity("Mentoría online para jóvenes",
                "Acompañamiento individual en la búsqueda del primer empleo.",
                "educar", "ONLINE", null, 4, 16,
                ActivityStatus.IN_PROGRESS, today.minusDays(5), today.plusDays(50),
                today.minusDays(15), partners.get(2), admin));

        activities.add(activity("Limpieza de playas",
                "Jornada de recogida de residuos en el litoral.",
                "medioambiente", "PRESENCIAL", "Valencia", 8, 6,
                ActivityStatus.PUBLISHED, today.plusDays(30), today.plusDays(30),
                today.plusDays(25), partners.get(5), admin));

        activities.add(activity("Reparto del banco de alimentos",
                "Clasificación y reparto de alimentos a familias en situación vulnerable.",
                "medioambiente", "PRESENCIAL", "Valencia", 6, 8,
                ActivityStatus.PUBLISHED, today.plusDays(40), today.plusDays(41),
                today.plusDays(30), partners.get(4), admin));

        activities.add(activity("Charlas de prevención",
                "Charlas en institutos sobre convivencia y prevención del acoso.",
                "acoso", "MIXTO", "Barcelona", 5, 4,
                ActivityStatus.PUBLISHED, today.plusDays(45), today.plusDays(45),
                today.plusDays(35), partners.get(3), admin));

        // Aforo 2 para que con ocho empleadas se pueda llenar de verdad y quede cola.
        activities.add(activity("Visitas a residencias",
                "Visitas de acompañamiento en residencias de mayores.",
                "desoledad", "PRESENCIAL", "Barcelona", 2, 12,
                ActivityStatus.FULL, today.plusDays(40), today.plusDays(60),
                today.plusDays(30), partners.get(0), admin));

        activities.add(activity("Seguridad en el hogar",
                "Curso en línea sobre prevención de riesgos domésticos.",
                "educar", "ONLINE", null, 10, 8,
                ActivityStatus.DRAFT, today.plusDays(60), today.plusDays(80),
                today.plusDays(40), partners.get(1), admin));

        // Propuesta por Cruz Roja y pendiente de que la Fundación la apruebe.
        activities.add(activity("Voluntariado ambiental",
                "Jornada de reforestación con voluntariado corporativo.",
                "medioambiente", "PRESENCIAL", "Madrid", 8, 6,
                ActivityStatus.PENDING_APPROVAL, today.plusDays(50), today.plusDays(50),
                today.plusDays(40), partners.get(5), redCrossUser));

        // Devuelta a borrador por la Fundación: es el único dato con nota de
        // revisión, y sin él la pantalla de la entidad nunca enseña ese estado.
        Activity returned = activity("Taller de escucha activa",
                "Formación para voluntariado que acompaña a personas mayores solas.",
                "desoledad", "PRESENCIAL", "Barcelona", 6, 8,
                ActivityStatus.DRAFT, today.plusDays(70), today.plusDays(72),
                today.plusDays(60), partners.get(0), caritasUser);
        returned.setReviewNote("Falta concretar el calendario y el centro; reenviad con fechas.");
        activities.add(returned);

        activityRepository.saveAll(activities);
    }

    private Activity activity(String title, String description, String line, String mode,
                         String location, int spots, int hours,
                         ActivityStatus status, LocalDate start, LocalDate end,
                         LocalDate deadline, Partner partner, User createdBy) {
        Activity a = new Activity();
        a.setTitle(title);
        a.setDescription(description);
        a.setLine(line);
        a.setMode(mode);
        a.setLocation(location);
        a.setSpots(spots);
        a.setHours(hours);
        a.setStatus(status);
        a.setStartDate(start);
        a.setEndDate(end);
        a.setRegistrationDeadline(deadline);
        a.setPartner(partner);
        a.setCreatedBy(createdBy);
        return a;
    }

    private User firstUserWithRole(Role role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == role)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "UserSeeder no dejó ninguna persona con rol " + role));
    }

    /** La primera persona de una entidad: en la semilla es siempre su cuenta ACTIVE. */
    private User partnerUserOf(Partner partner) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.PARTNER)
                .filter(u -> u.getPartner() != null && u.getPartner().getId().equals(partner.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "UserSeeder no dejó ninguna persona en la entidad " + partner.getName()));
    }
}
