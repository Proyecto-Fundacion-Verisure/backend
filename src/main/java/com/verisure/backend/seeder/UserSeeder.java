package com.verisure.backend.seeder;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.Organization;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.entity.enums.UserStatus;
import com.verisure.backend.repository.PartnerRepository;
import com.verisure.backend.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;

/**
 * Siembra las 19 personas, con los <b>tres</b> roles en un solo archivo.
 *
 * <p>Conviene no confundir dos cosas que comparten nombre: {@code Partner} es la
 * <b>organización</b> colaboradora —una fila con su CIF—, y {@code Role.PARTNER}
 * es el <b>rol de la persona</b> que trabaja en ella y entra a la aplicación.
 * {@code User.partner} es el puente entre las dos.
 *
 * <p>Orden 2 de siete: va después de {@link PartnerSeeder} porque las personas
 * con rol {@code PARTNER} llevan una clave ajena a su organización.
 *
 * <p>Dueñas: BE1 (B1-01) y BE3 (B3-15), que antes tenían un seeder cada una.
 */
@Component
@Order(2)
@Profile("!prod")
@RequiredArgsConstructor
public class UserSeeder implements CommandLineRunner {

    /**
     * Contraseña de todas las cuentas de demostración, en claro.
     *
     * <p>Se cifra con BCrypt antes de guardarla. Está documentada en el README
     * porque es un dato de demostración, no un secreto: estas cuentas solo
     * existen fuera de producción, gracias a {@code @Profile("!prod")}.
     */
    public static final String PASSWORD_DEMO = "Verisure2026!";

    private static final List<String> DEPARTMENTS = List.of(
            "Atención al Cliente", "Operaciones", "Tecnología",
            "Marketing", "Recursos Humanos", "Finanzas");

    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final PasswordEncoder passwordEncoder;

    /** Se calcula una vez en {@link #run(String...)} y la usan los ayudantes. */
    private String hashedPassword;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // idempotente: no duplica al reiniciar
        }

        // Se cifra una sola vez: BCrypt es deliberadamente lento, y hacerlo por
        // cada usuario alargaría el arranque sin ninguna ganancia, porque todas
        // las cuentas de demostración comparten contraseña.
        this.hashedPassword = passwordEncoder.encode(PASSWORD_DEMO);

        List<User> users = new ArrayList<>();

        // 2 ADMIN · la Fundación
        users.add(verisureUser("Carmen Ortega", "carmen.ortega@fundacionverisure.org",
                Role.ADMIN, Organization.VERISURE_ES, "Fundación"));
        users.add(verisureUser("Diego Salas", "diego.salas@fundacionverisure.org",
                Role.ADMIN, Organization.VERISURE_ES, "Fundación"));

        // 8 EMPLOYEE · 5 en VERISURE_ES y 3 en VERISURE_GROUP, los seis departamentos rotando
        String[][] employees = {
                {"Ana Gil",         "ana.gil"},
                {"Beatriz Nuño",    "beatriz.nuno"},
                {"Carlos Pena",     "carlos.pena"},
                {"Daniela Rueda",   "daniela.rueda"},
                {"Elena Sanz",      "elena.sanz"},
                {"Fernando Toro",   "fernando.toro"},
                {"Gema Ureña",      "gema.urena"},
                {"Hugo Valle",      "hugo.valle"},
        };
        for (int i = 0; i < employees.length; i++) {
            users.add(verisureUser(
                    employees[i][0],
                    employees[i][1] + "@verisure.es",
                    Role.EMPLOYEE,
                    i < 5 ? Organization.VERISURE_ES : Organization.VERISURE_GROUP,
                    DEPARTMENTS.get(i % DEPARTMENTS.size())));
        }

        // 9 PARTNER · una por organización, más una segunda cuenta de Cáritas.
        // Cubren los cuatro UserStatus.
        List<Partner> partners = partnerRepository.findAll();
        String[][] partnerPeople = {
                {"Marta Ribas",    "marta.ribas@caritasbcn.org",             "Cáritas Barcelona"},
                {"Lucía Ferrer",   "lucia.ferrer@fundacionsolitaria.org",    "Fundación Solitaria"},
                {"Andrés Molina",  "andres.molina@educamosjuntos.org",       "Educamos Juntos"},
                {"Nuria Camps",    "nuria.camps@prevenciontotal.org",        "Prevención Total"},
                {"Pilar Server",   "pilar.server@bancoalimentos.org",        "Banco de Alimentos"},
                {"Jorge Ibáñez",   "jorge.ibanez@cruzroja.org",              "Cruz Roja Valencia"},
        };
        for (String[] row : partnerPeople) {
            users.add(partnerUser(row[0], row[1],
                    findByName(partners, row[2]), UserStatus.ACTIVE));
        }

        // Segunda cuenta de una organización que ya está ACTIVE: es el caso de la regla
        // del CIF ya registrado, que sin este dato no se puede probar.
        users.add(partnerUser("Pau Estévez", "pau.estevez@caritasbcn.org",
                findByName(partners, "Cáritas Barcelona"), UserStatus.PENDING_VERIFICATION));

        users.add(partnerUser("Elena Vargas", "elena.vargas@aldeasinfantiles.org",
                findByName(partners, "Aldeas Infantiles"), UserStatus.PENDING_APPROVAL));

        users.add(partnerUser("Rosa Delgado", "rosa.delgado@manosunidas.org",
                findByName(partners, "Manos Unidas"), UserStatus.REJECTED));

        userRepository.saveAll(users);
    }

    /** Personal de Verisure: pertenece a una organización y no tiene entidad. */
    private User verisureUser(String fullName, String email, Role role,
                         Organization organization, String department) {
        User u = base(fullName, email, role);
        u.setOrganization(organization);
        u.setDepartment(department);
        u.setPartner(null);
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    /**
     * Persona de una entidad colaboradora. {@code organization} y
     * {@code department} van a {@code null}: no pertenece ni a Verisure España ni
     * a Verisure Grupo, y por eso los agregados del dashboard la excluyen.
     */
    private User partnerUser(String fullName, String email, Partner partner, UserStatus status) {
        User u = base(fullName, email, Role.PARTNER);
        u.setOrganization(null);
        u.setDepartment(null);
        u.setPartner(partner);
        u.setStatus(status);
        return u;
    }

    private User base(String fullName, String email, Role role) {
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPassword(hashedPassword);
        u.setRole(role);
        return u;
    }

    private Partner findByName(List<Partner> partners, String name) {
        return partners.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "PartnerSeeder no dejó sembrada la organización: " + name));
    }
}
