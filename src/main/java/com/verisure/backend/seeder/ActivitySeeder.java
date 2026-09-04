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
 * Siembra las 12 actividades: tres por cada línea de acción y los
 * <b>siete</b> {@code ActivityStatus} representados.
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
 * <p>Dueña: BE2 · Tarea: B2-01
 */
@Component
@Order(3)
@Profile("!prod")
@RequiredArgsConstructor
public class ActivitySeeder implements CommandLineRunner {

    private static final String IMG_DESOLEDAD     = "/images/01-desoledad-linea-de-accion.png";
    private static final String IMG_EDUCAR        = "/images/02-educar-linea-de-accion.png";
    private static final String IMG_ACOSO         = "/images/03-acoso-linea-de-accion.png";
    // El archivo conserva el nombre antiguo: la imagen la sirve frontend y
    // renombrarla desde aquí dejaría la portada rota hasta que ellos la cambien.
    private static final String IMG_MEDIOAMBIENTE = "/images/04-voluntariado-linea-de-accion.png";

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
        // La actividad PENDING_APPROVAL la propone una partnerUser, no la Fundación.
        User partnerUser = firstUserWithRole(Role.PARTNER);

        LocalDate today = LocalDate.now();
        List<Activity> activities = new ArrayList<>();

        // ── Pasadas · fechas fijas de 2026 ──────────────────────────────────────
        // La 1 es la que sostiene el dashboard: sus inscripciones acaban en CLOSED.
        activities.add(activity("Acompañamiento a mayores",
                "Visitas semanales a personas mayores en situación de soledad no deseada.",
                "desoledad", "PRESENCIAL", "Barcelona", 8, 20, IMG_DESOLEDAD,
                ActivityStatus.FINISHED, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 27),
                LocalDate.of(2026, 2, 20), partners.get(0), admin));

        activities.add(activity("Alfabetización digital",
                "Talleres para que personas mayores aprendan a usar el móvil y la banca en línea.",
                "desoledad", "MIXTO", "Madrid", 6, 12, IMG_DESOLEDAD,
                ActivityStatus.FINISHED, LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 24),
                LocalDate.of(2026, 3, 25), partners.get(1), admin));

        activities.add(activity("Campaña contra el acoso escolar",
                "Sensibilización en centros educativos sobre el acoso entre iguales.",
                "acoso", "PRESENCIAL", "Barcelona", 5, 10, IMG_ACOSO,
                ActivityStatus.FINISHED, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 22),
                LocalDate.of(2026, 4, 22), partners.get(3), admin));

        activities.add(activity("Autoprotección para adolescentes",
                "Taller de autoprotección y uso seguro de redes sociales.",
                "acoso", "MIXTO", "Sevilla", 6, 10, IMG_ACOSO,
                ActivityStatus.CANCELLED, LocalDate.of(2026, 2, 9), LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 1, 30), partners.get(3), admin));

        // ── Vivas · fechas relativas a today ──────────────────────────────────────
        activities.add(activity("Refuerzo escolar",
                "Apoyo escolar para menores en riesgo de exclusión.",
                "educar", "PRESENCIAL", "Madrid", 6, 30, IMG_EDUCAR,
                ActivityStatus.IN_PROGRESS, today.minusDays(10), today.plusDays(20),
                today.minusDays(20), partners.get(2), admin));

        activities.add(activity("Mentoría online para jóvenes",
                "Acompañamiento individual en la búsqueda del primer empleo.",
                "educar", "ONLINE", null, 4, 16, IMG_EDUCAR,
                ActivityStatus.IN_PROGRESS, today.minusDays(5), today.plusDays(25),
                today.minusDays(15), partners.get(2), admin));

        activities.add(activity("Limpieza de playas",
                "Jornada de recogida de residuos en el litoral.",
                "medioambiente", "PRESENCIAL", "Valencia", 8, 6, IMG_MEDIOAMBIENTE,
                ActivityStatus.PUBLISHED, today.plusDays(20), today.plusDays(20),
                today.plusDays(12), partners.get(5), admin));

        activities.add(activity("Reparto del banco de alimentos",
                "Clasificación y reparto de alimentos a familias en situación vulnerable.",
                "medioambiente", "PRESENCIAL", "Valencia", 6, 8, IMG_MEDIOAMBIENTE,
                ActivityStatus.PUBLISHED, today.plusDays(30), today.plusDays(31),
                today.plusDays(21), partners.get(4), admin));

        activities.add(activity("Charlas de prevención",
                "Charlas en institutos sobre convivencia y prevención del acoso.",
                "acoso", "MIXTO", "Barcelona", 5, 4, IMG_ACOSO,
                ActivityStatus.PUBLISHED, today.plusDays(40), today.plusDays(40),
                today.plusDays(30), partners.get(3), admin));

        // Aforo 2 para que con ocho empleadas se pueda llenar de verdad y quede cola.
        activities.add(activity("Visitas a residencias",
                "Visitas de acompañamiento en residencias de mayores.",
                "desoledad", "PRESENCIAL", "Barcelona", 2, 12, IMG_DESOLEDAD,
                ActivityStatus.FULL, today.plusDays(15), today.plusDays(45),
                today.plusDays(7), partners.get(0), admin));

        activities.add(activity("Seguridad en el hogar",
                "Curso en línea sobre prevención de riesgos domésticos.",
                "educar", "ONLINE", null, 10, 8, IMG_EDUCAR,
                ActivityStatus.DRAFT, today.plusDays(60), today.plusDays(62),
                today.plusDays(50), partners.get(1), admin));

        // Propuesta por una partnerUser y pendiente de que la Fundación la apruebe.
        activities.add(activity("Voluntariado ambiental",
                "Jornada de reforestación con voluntariado corporativo.",
                "medioambiente", "PRESENCIAL", "Madrid", 8, 6, IMG_MEDIOAMBIENTE,
                ActivityStatus.PENDING_APPROVAL, today.plusDays(50), today.plusDays(50),
                today.plusDays(40), partners.get(5), partnerUser));

        activityRepository.saveAll(activities);
    }

    private Activity activity(String title, String description, String line, String mode,
                         String location, int spots, int hours, String imageUrl,
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
        a.setImageUrl(imageUrl);
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
}
