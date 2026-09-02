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

    /** TODO C-04 · el cifrado y el PasswordEncoder son del paso 2 de C-04. */
    private static final String PASSWORD_SIN_CIFRAR = "TODO-C04-sin-cifrar";

    private static final List<String> DEPARTAMENTOS = List.of(
            "Atención al Cliente", "Operaciones", "Tecnología",
            "Marketing", "Recursos Humanos", "Finanzas");

    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // idempotente: no duplica al reiniciar
        }

        List<User> usuarios = new ArrayList<>();

        // 2 ADMIN · la Fundación
        usuarios.add(interno("Carmen Ortega", "carmen.ortega@fundacionverisure.org",
                Role.ADMIN, Organization.VERISURE_ES, "Fundación"));
        usuarios.add(interno("Diego Salas", "diego.salas@fundacionverisure.org",
                Role.ADMIN, Organization.VERISURE_ES, "Fundación"));

        // 8 EMPLOYEE · 5 en VERISURE_ES y 3 en VERISURE_GROUP, los seis departamentos rotando
        String[][] empleadas = {
                {"Ana Gil",         "ana.gil"},
                {"Beatriz Nuño",    "beatriz.nuno"},
                {"Carlos Pena",     "carlos.pena"},
                {"Daniela Rueda",   "daniela.rueda"},
                {"Elena Sanz",      "elena.sanz"},
                {"Fernando Toro",   "fernando.toro"},
                {"Gema Ureña",      "gema.urena"},
                {"Hugo Valle",      "hugo.valle"},
        };
        for (int i = 0; i < empleadas.length; i++) {
            usuarios.add(interno(
                    empleadas[i][0],
                    empleadas[i][1] + "@verisure.es",
                    Role.EMPLOYEE,
                    i < 5 ? Organization.VERISURE_ES : Organization.VERISURE_GROUP,
                    DEPARTAMENTOS.get(i % DEPARTAMENTOS.size())));
        }

        // 9 PARTNER · una por organización, más una segunda cuenta de Cáritas.
        // Cubren los cuatro UserStatus.
        List<Partner> organizaciones = partnerRepository.findAll();
        String[][] personasDeEntidad = {
                {"Marta Ribas",    "marta.ribas@caritasbcn.org",             "Cáritas Barcelona"},
                {"Lucía Ferrer",   "lucia.ferrer@fundacionsolitaria.org",    "Fundación Solitaria"},
                {"Andrés Molina",  "andres.molina@educamosjuntos.org",       "Educamos Juntos"},
                {"Nuria Camps",    "nuria.camps@prevenciontotal.org",        "Prevención Total"},
                {"Pilar Server",   "pilar.server@bancoalimentos.org",        "Banco de Alimentos"},
                {"Jorge Ibáñez",   "jorge.ibanez@cruzroja.org",              "Cruz Roja Valencia"},
        };
        for (String[] persona : personasDeEntidad) {
            usuarios.add(deEntidad(persona[0], persona[1],
                    buscar(organizaciones, persona[2]), UserStatus.ACTIVE));
        }

        // Segunda cuenta de una organización que ya está ACTIVE: es el caso de la regla
        // del CIF ya registrado, que sin este dato no se puede probar.
        usuarios.add(deEntidad("Pau Estévez", "pau.estevez@caritasbcn.org",
                buscar(organizaciones, "Cáritas Barcelona"), UserStatus.PENDING_VERIFICATION));

        usuarios.add(deEntidad("Elena Vargas", "elena.vargas@aldeasinfantiles.org",
                buscar(organizaciones, "Aldeas Infantiles"), UserStatus.PENDING_APPROVAL));

        usuarios.add(deEntidad("Rosa Delgado", "rosa.delgado@manosunidas.org",
                buscar(organizaciones, "Manos Unidas"), UserStatus.REJECTED));

        userRepository.saveAll(usuarios);
    }

    /** Personal de Verisure: pertenece a una organización y no tiene entidad. */
    private User interno(String fullName, String email, Role role,
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
    private User deEntidad(String fullName, String email, Partner partner, UserStatus status) {
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
        u.setPassword(PASSWORD_SIN_CIFRAR);
        u.setRole(role);
        return u;
    }

    private Partner buscar(List<Partner> organizaciones, String nombre) {
        return organizaciones.stream()
                .filter(p -> p.getName().equals(nombre))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "PartnerSeeder no dejó sembrada la organización: " + nombre));
    }
}
