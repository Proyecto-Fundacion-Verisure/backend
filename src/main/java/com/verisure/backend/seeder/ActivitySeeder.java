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
 * a hoy, para que siempre haya plazo de inscripción abierto.
 *
 * <p>Dueña: BE2 · Tarea: B2-01
 */
@Component
@Order(3)
@Profile("!prod")
@RequiredArgsConstructor
public class ActivitySeeder implements CommandLineRunner {

    private static final String IMG_DESOLEDAD    = "/images/01-desoledad-linea-de-accion.png";
    private static final String IMG_EDUCAR       = "/images/02-educar-linea-de-accion.png";
    private static final String IMG_ACOSO        = "/images/03-acoso-linea-de-accion.png";
    private static final String IMG_VOLUNTARIADO = "/images/04-voluntariado-linea-de-accion.png";

    private final ActivityRepository activityRepository;
    private final PartnerRepository partnerRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (activityRepository.count() > 0) {
            return; // idempotente: no duplica al reiniciar
        }

        List<Partner> orgs = partnerRepository.findAll();
        User admin = primeraConRol(Role.ADMIN);
        // La actividad PENDING_APPROVAL la propone una entidad, no la Fundación.
        User entidad = primeraConRol(Role.PARTNER);

        LocalDate hoy = LocalDate.now();
        List<Activity> actividades = new ArrayList<>();

        // ── Pasadas · fechas fijas de 2026 ──────────────────────────────────────
        // La 1 es la que sostiene el dashboard: sus inscripciones acaban en CLOSED.
        actividades.add(act("Acompañamiento a mayores",
                "Visitas semanales a personas mayores en situación de soledad no deseada.",
                "desoledad", "PRESENCIAL", "Barcelona", 8, 20, IMG_DESOLEDAD,
                ActivityStatus.FINISHED, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 27),
                LocalDate.of(2026, 2, 20), orgs.get(0), admin));

        actividades.add(act("Alfabetización digital",
                "Talleres para que personas mayores aprendan a usar el móvil y la banca en línea.",
                "desoledad", "MIXTO", "Madrid", 6, 12, IMG_DESOLEDAD,
                ActivityStatus.FINISHED, LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 24),
                LocalDate.of(2026, 3, 25), orgs.get(1), admin));

        actividades.add(act("Campaña contra el acoso escolar",
                "Sensibilización en centros educativos sobre el acoso entre iguales.",
                "acoso", "PRESENCIAL", "Barcelona", 5, 10, IMG_ACOSO,
                ActivityStatus.FINISHED, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 22),
                LocalDate.of(2026, 4, 22), orgs.get(3), admin));

        actividades.add(act("Autoprotección para adolescentes",
                "Taller de autoprotección y uso seguro de redes sociales.",
                "acoso", "MIXTO", "Sevilla", 6, 10, IMG_ACOSO,
                ActivityStatus.CANCELLED, LocalDate.of(2026, 2, 9), LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 1, 30), orgs.get(3), admin));

        // ── Vivas · fechas relativas a hoy ──────────────────────────────────────
        actividades.add(act("Refuerzo escolar",
                "Apoyo escolar para menores en riesgo de exclusión.",
                "educar", "PRESENCIAL", "Madrid", 6, 30, IMG_EDUCAR,
                ActivityStatus.IN_PROGRESS, hoy.minusDays(10), hoy.plusDays(20),
                hoy.minusDays(20), orgs.get(2), admin));

        actividades.add(act("Mentoría online para jóvenes",
                "Acompañamiento individual en la búsqueda del primer empleo.",
                "educar", "ONLINE", "Online", 4, 16, IMG_EDUCAR,
                ActivityStatus.IN_PROGRESS, hoy.minusDays(5), hoy.plusDays(25),
                hoy.minusDays(15), orgs.get(2), admin));

        actividades.add(act("Limpieza de playas",
                "Jornada de recogida de residuos en el litoral.",
                "voluntariado", "PRESENCIAL", "Valencia", 8, 6, IMG_VOLUNTARIADO,
                ActivityStatus.PUBLISHED, hoy.plusDays(20), hoy.plusDays(20),
                hoy.plusDays(12), orgs.get(5), admin));

        actividades.add(act("Reparto del banco de alimentos",
                "Clasificación y reparto de alimentos a familias en situación vulnerable.",
                "voluntariado", "PRESENCIAL", "Valencia", 6, 8, IMG_VOLUNTARIADO,
                ActivityStatus.PUBLISHED, hoy.plusDays(30), hoy.plusDays(31),
                hoy.plusDays(21), orgs.get(4), admin));

        actividades.add(act("Charlas de prevención",
                "Charlas en institutos sobre convivencia y prevención del acoso.",
                "acoso", "MIXTO", "Barcelona", 5, 4, IMG_ACOSO,
                ActivityStatus.PUBLISHED, hoy.plusDays(40), hoy.plusDays(40),
                hoy.plusDays(30), orgs.get(3), admin));

        // Aforo 2 para que con ocho empleadas se pueda llenar de verdad y quede cola.
        actividades.add(act("Visitas a residencias",
                "Visitas de acompañamiento en residencias de mayores.",
                "desoledad", "PRESENCIAL", "Barcelona", 2, 12, IMG_DESOLEDAD,
                ActivityStatus.FULL, hoy.plusDays(15), hoy.plusDays(45),
                hoy.plusDays(7), orgs.get(0), admin));

        actividades.add(act("Seguridad en el hogar",
                "Curso en línea sobre prevención de riesgos domésticos.",
                "educar", "ONLINE", "Online", 10, 8, IMG_EDUCAR,
                ActivityStatus.DRAFT, hoy.plusDays(60), hoy.plusDays(62),
                hoy.plusDays(50), orgs.get(1), admin));

        // Propuesta por una entidad y pendiente de que la Fundación la apruebe.
        actividades.add(act("Voluntariado ambiental",
                "Jornada de reforestación con voluntariado corporativo.",
                "voluntariado", "PRESENCIAL", "Madrid", 8, 6, IMG_VOLUNTARIADO,
                ActivityStatus.PENDING_APPROVAL, hoy.plusDays(50), hoy.plusDays(50),
                hoy.plusDays(40), orgs.get(5), entidad));

        activityRepository.saveAll(actividades);
    }

    private Activity act(String title, String description, String line, String mode,
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

    private User primeraConRol(Role rol) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == rol)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "UserSeeder no dejó ninguna persona con rol " + rol));
    }
}
