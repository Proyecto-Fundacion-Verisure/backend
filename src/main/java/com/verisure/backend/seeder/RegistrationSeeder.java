package com.verisure.backend.seeder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Siembra 13 registrations con los seis {@code RegistrationStatus}
 * representados.
 *
 * <p>Es el seeder del que depende la demo entera: las seis registrations en
 * {@code CLOSED} —tres de «Acompañamiento a mayores» y tres de «Campaña contra el
 * acoso escolar»— son las únicas filas que ve el dashboard, porque
 * {@code findDashboardData} filtra por ese estado. Si alguien las cambia, el
 * dashboard sale vacío y nada lo avisa. Van en dos actividades de línea, entidad
 * y trimestre distintos, y con gente de las dos organizaciones, para que las
 * distribuciones y la variación trimestral no salgan de una sola porción.
 *
 * <p>Fernando Toro (la sexta empleada) no tiene ninguna inscripción a propósito:
 * es quien se apunta en vivo en la demo.
 *
 * <p>Dentro de una misma actividad no se repite ninguna empleada: la regla «no
 * volver a inscribirse» es un índice parcial que JPA no puede declarar, así que
 * nada impediría duplicar el par a mano.
 *
 * <p>Dueña: BE3 · Tarea: B3-01
 */
@Component
@Order(5)
@Profile("!prod")
@RequiredArgsConstructor
public class RegistrationSeeder implements CommandLineRunner {

    private final RegistrationRepository registrationRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (registrationRepository.count() > 0) {
            return; // idempotente: no duplica al reiniciar
        }

        List<User> employees = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .toList();
        User admin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("UserSeeder no dejó ninguna ADMIN"));

        Activity elderlySupport = activityByTitle("Acompañamiento a mayores");
        Activity bullyingCampaign = activityByTitle("Campaña contra el acoso escolar");
        Activity digitalLiteracy = activityByTitle("Alfabetización digital");
        Activity careHomeVisits = activityByTitle("Visitas a residencias");
        Activity beachCleanup = activityByTitle("Limpieza de playas");
        Activity selfProtection = activityByTitle("Autoprotección para adolescentes");

        List<Registration> registrations = new ArrayList<>();

        // 3 CLOSED en «Acompañamiento a mayores» · primer trimestre, VERISURE_ES
        for (int i = 0; i < 3; i++) {
            registrations.add(registration(elderlySupport, employees.get(i), RegistrationStatus.CLOSED,
                    true, null, admin, LocalDate.of(2026, 2, 10)));
        }

        // 3 CLOSED en «Campaña contra el acoso escolar» · segundo trimestre, con dos
        // de VERISURE_GROUP (las empleadas 6 y 7). Nunca la 5, que es Fernando.
        for (int i : new int[] {2, 6, 7}) {
            registrations.add(registration(bullyingCampaign, employees.get(i), RegistrationStatus.CLOSED,
                    true, null, admin, LocalDate.of(2026, 4, 15)));
        }

        // 2 PENDING_CLOSURE · la cola de cierres de administración
        for (int i = 0; i < 2; i++) {
            registrations.add(registration(digitalLiteracy, employees.get(i), RegistrationStatus.PENDING_CLOSURE,
                    true, null, admin, LocalDate.of(2026, 3, 15)));
        }

        // Actividad FULL: 2 confirmadas (= aforo) y 3 en cola, 1 revisada y 2 sin revisar
        registrations.add(registration(careHomeVisits, employees.get(0), RegistrationStatus.CONFIRMED,
                true, null, admin, LocalDate.now().minusDays(9)));
        registrations.add(registration(careHomeVisits, employees.get(1), RegistrationStatus.CONFIRMED,
                true, null, admin, LocalDate.now().minusDays(9)));
        // accepted = true aunque esté en cola: pasó por administración, pero no había hueco
        registrations.add(registration(careHomeVisits, employees.get(2), RegistrationStatus.WAITLISTED,
                true, 1, admin, LocalDate.now().minusDays(8)));
        // Sin revisar: nadie las ha decidido todavía. Son las únicas que hacen aparecer
        // los botones de aceptar y rechazar en el tablero, así que sin ellas esa sección
        // sale vacía al arrancar y no se puede probar la decisión sin inscribirse antes.
        registrations.add(registration(careHomeVisits, employees.get(3), RegistrationStatus.WAITLISTED,
                false, 2, null, LocalDate.now().minusDays(2)));
        registrations.add(registration(careHomeVisits, employees.get(4), RegistrationStatus.WAITLISTED,
                false, 3, null, LocalDate.now().minusDays(1)));

        // 1 REJECTED · rechazar no admite motivo, por contrato
        registrations.add(registration(beachCleanup, employees.get(3), RegistrationStatus.REJECTED,
                false, null, admin, LocalDate.now().minusDays(3)));

        // 1 CANCELLED · como si la hubiera cancelado cancelAllForActivity
        registrations.add(registration(selfProtection, employees.get(4), RegistrationStatus.CANCELLED,
                true, null, admin, LocalDate.of(2026, 1, 28)));

        registrationRepository.saveAll(registrations);
    }

    private Registration registration(Activity activity, User user, RegistrationStatus status,
                              boolean accepted, Integer queuePosition, User decidedBy,
                              LocalDate createdOn) {
        Registration r = new Registration();
        r.setActivity(activity);
        r.setUser(user);
        r.setStatus(status);
        r.setAccepted(accepted);
        r.setQueuePosition(queuePosition);
        r.setCreatedAt(instantOf(createdOn));
        // Solo llevan decisor las que alguien decidió; una WAITLISTED sin revisar no lo tendría.
        boolean decided = status != RegistrationStatus.WAITLISTED || accepted;
        r.setDecidedBy(decided ? decidedBy : null);
        r.setDecidedAt(decided ? instantOf(createdOn.plusDays(1)) : null);
        return r;
    }

    private Activity activityByTitle(String title) {
        return activityRepository.findAll().stream()
                .filter(a -> a.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "ActivitySeeder no dejó sembrada la actividad: " + title));
    }

    private Instant instantOf(LocalDate date) {
        return date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
