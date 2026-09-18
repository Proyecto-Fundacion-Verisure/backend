package com.verisure.backend.support;

import java.time.Instant;
import java.time.LocalDate;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.entity.enums.Organization;
import com.verisure.backend.entity.enums.PartnerStatus;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.entity.enums.UserStatus;

/**
 * Entidades en memoria para los tests, con el identificador puesto a mano.
 *
 * <p>No guardan nada: están pensadas para devolverlas desde un repositorio
 * mockeado con {@code when(repo.findById(id)).thenReturn(Optional.of(x))}.
 */
public final class TestData {

    public static final String PASSWORD = "Verisure2026!";

    private TestData() {
    }

    public static User admin() {
        return user(1L, "carmen.ortega@verisure.ex", Role.ADMIN, UserStatus.ACTIVE);
    }

    public static User employee(Long id, String email) {
        return user(id, email, Role.EMPLOYEE, UserStatus.ACTIVE);
    }

    public static User user(Long id, String email, Role role, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setFullName("Persona " + id);
        user.setEmail(email);
        user.setPassword("hash");
        user.setRole(role);
        user.setStatus(status);
        user.setOrganization(role == Role.PARTNER ? null : Organization.VERISURE_ES);
        user.setDepartment(role == Role.PARTNER ? null : "Tecnología");
        user.setCreatedAt(Instant.now());
        return user;
    }

    public static Partner partner(Long id, String name) {
        Partner partner = new Partner();
        partner.setId(id);
        partner.setName(name);
        partner.setCif("B" + String.format("%08d", id));
        partner.setEmail("contacto@" + id + ".ex");
        partner.setStatus(PartnerStatus.ACTIVE);
        partner.setCreatedAt(Instant.now());
        return partner;
    }

    /** Actividad que empieza dentro de diez días y admite inscripciones hasta mañana. */
    public static Activity activity(Long id, ActivityStatus status, int spots) {
        LocalDate start = LocalDate.now().plusDays(10);
        return activity(id, status, spots, start, start.plusDays(1), LocalDate.now().plusDays(1));
    }

    public static Activity activity(Long id, ActivityStatus status, int spots,
                                    LocalDate startDate, LocalDate endDate,
                                    LocalDate registrationDeadline) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setTitle("Actividad " + id);
        activity.setDescription("Descripción");
        activity.setLine("educar");
        activity.setMode("PRESENCIAL");
        activity.setLocation("Madrid");
        activity.setStartDate(startDate);
        activity.setEndDate(endDate);
        activity.setRegistrationDeadline(registrationDeadline);
        activity.setHours(4);
        activity.setSpots(spots);
        activity.setStatus(status);
        activity.setPartner(partner(1L, "Cáritas"));
        return activity;
    }

    public static Registration registration(Long id, User user, Activity activity,
                                            RegistrationStatus status, boolean accepted,
                                            Integer queuePosition) {
        Registration registration = new Registration();
        registration.setId(id);
        registration.setUser(user);
        registration.setActivity(activity);
        registration.setStatus(status);
        registration.setAccepted(accepted);
        registration.setQueuePosition(queuePosition);
        registration.setCreatedAt(Instant.now());
        return registration;
    }
}
