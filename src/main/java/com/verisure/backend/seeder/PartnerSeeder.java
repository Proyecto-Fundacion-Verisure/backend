package com.verisure.backend.seeder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.enums.PartnerStatus;
import com.verisure.backend.repository.PartnerRepository;

import lombok.RequiredArgsConstructor;

/**
 * Siembra las 8 entidades colaboradoras.
 *
 * <p>Orden 1 de siete: va primero porque {@code Partner} no tiene claves ajenas
 * salientes y porque las personas con rol {@code PARTNER} apuntan a una de estas
 * filas.
 *
 * <p>Los tres {@code PartnerStatus} están representados: la que está en
 * {@code PENDING} alimenta la bandeja de {@code GET /api/admin/org-accounts} y
 * la {@code REJECTED} sirve para comprobar que no aparece donde no debe.
 *
 * <p>Dueña: BE2 · Tarea: B2-12
 */
@Component
@Order(1)
@Profile("!prod")
@RequiredArgsConstructor
public class PartnerSeeder implements CommandLineRunner {

    private final PartnerRepository partnerRepository;

    @Override
    public void run(String... args) {
        if (partnerRepository.count() > 0) {
            return; // idempotente: no duplica al reiniciar
        }

        partnerRepository.saveAll(List.of(
                partner("Cáritas Barcelona", "G08123456", "Marta Ribas",
                        "contacto@caritasbcn.org", "934112233", PartnerStatus.ACTIVE, 1),
                partner("Fundación Solitaria", "G28234567", "Lucía Ferrer",
                        "hola@fundacionsolitaria.org", "915223344", PartnerStatus.ACTIVE, 2),
                partner("Educamos Juntos", "G28345678", "Andrés Molina",
                        "info@educamosjuntos.org", "915334455", PartnerStatus.ACTIVE, 3),
                partner("Prevención Total", "G08456789", "Nuria Camps",
                        "contacto@prevenciontotal.org", "934445566", PartnerStatus.ACTIVE, 4),
                partner("Banco de Alimentos", "G46567890", "Pilar Server",
                        "voluntariado@bancoalimentos.org", "963556677", PartnerStatus.ACTIVE, 5),
                partner("Cruz Roja Valencia", "G46678901", "Jorge Ibáñez",
                        "valencia@cruzroja.org", "963667788", PartnerStatus.ACTIVE, 6),
                partner("Aldeas Infantiles", "G28789012", "Elena Vargas",
                        "info@aldeasinfantiles.org", "915778899", PartnerStatus.PENDING, 7),
                partner("Manos Unidas", "G41890123", "Rosa Delgado",
                        "contacto@manosunidas.org", "954889900", PartnerStatus.REJECTED, 8)));
    }

    /** {@code diaDeEnero} escalona los {@code createdAt} sin usar aleatoriedad. */
    private Partner partner(String name, String cif, String contactName,
                            String email, String phone, PartnerStatus status, int diaDeEnero) {
        Partner p = new Partner();
        p.setName(name);
        p.setCif(cif);
        p.setContactName(contactName);
        p.setEmail(email);
        p.setPhone(phone);
        p.setStatus(status);
        p.setCreatedAt(instante(diaDeEnero));
        return p;
    }

    private Instant instante(int diaDeEnero) {
        return LocalDate.of(2026, 1, diaDeEnero).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
