package com.verisure.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.verisure.backend.dto.org.CreateOrgProposalRequest;
import com.verisure.backend.dto.org.OrgProposalRow;

/**
 * Las propuestas del rol entidad · {@code B2-16}.
 *
 * <p>Van a la misma tabla y a la misma bandeja que las del formulario público:
 * dos caminos separados acabarían en dos bandejas que no cuadran.
 *
 * <p>Dueña: BE3 · Tarea: B2-16.
 */
public interface OrgProposalService {

    /** Una página de las propuestas que ha escrito la entidad. */
    Page<OrgProposalRow> list(Pageable pageable, String userEmail);

    /** Crea una propuesta a nombre de la entidad, en estado {@code NEW}. */
    OrgProposalRow create(CreateOrgProposalRequest request, String userEmail);
}
