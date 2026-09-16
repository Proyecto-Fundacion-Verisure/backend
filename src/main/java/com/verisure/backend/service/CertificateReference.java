package com.verisure.backend.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.verisure.backend.entity.ParticipationClosure;

/**
 * Referencia única del certificado: {@code CERT-YYYY-NNNN}.
 *
 * <p>El año es el de la presentación del cierre ({@code submittedAt}) en UTC y
 * la secuencia el id del propio cierre: determinista, único y estable para
 * siempre. Se genera una sola vez, al cerrar la actividad · B1-21.
 */
public final class CertificateReference {

    private CertificateReference() {
    }

    public static String forClosure(ParticipationClosure closure) {
        int year = ZonedDateTime.ofInstant(closure.getSubmittedAt(), ZoneOffset.UTC).getYear();
        return String.format("CERT-%d-%04d", year, closure.getId());
    }
}