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
 * Siembra 10 registrations con los <b>seis</b> {@code RegistrationStatus}
 * representados.
 *
 * <p>Es el seeder del que depende la demo entera: las tres registrations en
 * {@code CLOSED} de «Acompañamiento a mayores» son <b>las únicas filas que ve el
 * dashboard</b>, porque {@code findClosedForDashboard} filtra por ese estado. Si
 * alguien las cambia, el dashboard sale vacío y nada lo avisa.
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
        Activity digitalLiteracy = activityByTitle("Alfabetización digital");
        Activity careHomeVisits    = activityByTitle("Visitas a residencias");
        Activity beachCleanup         = activityByTitle("Limpieza de playas");
        Activity selfProtection = activityByTitle("Autoprotección para adolescentes");

        List<Registration> registrations = new ArrayList<>();

        // 3 CLOSED · las únicas que alimentan el dashboard
        for (int i = 0; i < 3; i++) {
            registrations.add(registration(elderlySupport, employees.get(i), RegistrationStatus.CLOSED,
                    true, null, admin, LocalDate.of(2026, 2, 10)));
        }

        // 2 PENDING_CLOSURE · la cola de cierres de administración
        for (int i = 0; i < 2; i++) {
            registrations.add(registration(digitalLiteracy, employees.get(i), RegistrationStatus.PENDING_CLOSURE,
                    true, null, admin, LocalDate.of(2026, 3, 15)));
        }

        // Actividad FULL: 2 confirmadas (= aforo) y 1 en cola
        registrations.add(registration(careHomeVisits, employees.get(0), RegistrationStatus.CONFIRMED,
                true, null, admin, LocalDate.now().minusDays(9)));
        registrations.add(registration(careHomeVisits, employees.get(1), RegistrationStatus.CONFIRMED,
                true, null, admin, LocalDate.now().minusDays(9)));
        // accepted = true aunque esté en cola: pasó por administración, pero no había hueco
        registrations.add(registration(careHomeVisits, employees.get(2), RegistrationStatus.WAITLISTED,
                true, 1, admin, LocalDate.now().minusDays(8)));

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
