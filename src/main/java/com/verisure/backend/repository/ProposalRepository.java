package com.verisure.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verisure.backend.dto.org.OrgProposalRow;
import com.verisure.backend.dto.proposal.ProposalRow;
import com.verisure.backend.entity.Proposal;
import com.verisure.backend.entity.enums.ProposalStatus;

public interface ProposalRepository extends JpaRepository<Proposal, Long>{

    /**
     * Una página de la bandeja, filtrada por estado si llega.
     *
     * <p>Las dos relaciones van en {@code left join}: la entidad falta en las
     * propuestas que llegan por el formulario público, y la actividad solo
     * existe en las aceptadas. Con un {@code join} interno la bandeja perdería
     * justo las propuestas nuevas sin dueño, que son las que hay que atender.
     */
    @Query(value = """
            select new com.verisure.backend.dto.proposal.ProposalRow(
                p.id, pa.name, p.suggestedLine, p.estimatedVolunteers,
                p.scope, p.status, p.createdAt, a.id)
            from Proposal p
            left join p.partner pa
            left join p.activity a
            where (:status is null or p.status = :status)
            order by p.createdAt desc
            """,
            countQuery = """
            select count(p.id)
            from Proposal p
            where (:status is null or p.status = :status)
            """)
    Page<ProposalRow> findRows(@Param("status") ProposalStatus status, Pageable pageable);

    /**
     * La propuesta con su entidad ya cargada.
     *
     * <p>La ficha y la aceptación leen los datos de contacto y la entidad a la
     * que colgar la actividad, así que se traen en la misma consulta.
     */
    @Query("""
            select p from Proposal p
            left join fetch p.partner
            left join fetch p.activity
            where p.id = :proposalId
            """)
    Optional<Proposal> findByIdWithPartner(@Param("proposalId") Long proposalId);

    /**
     * Las propuestas de una entidad · {@code B2-16}.
     *
     * <p>La actividad va en {@code left join}: solo existe en las aceptadas, y
     * con un {@code join} interno la entidad solo vería esas.
     */
    @Query(value = """
            select new com.verisure.backend.dto.org.OrgProposalRow(
                p.id, p.description, p.suggestedLine, p.estimatedVolunteers,
                p.scope, p.status, p.createdAt, a.id)
            from Proposal p
            left join p.activity a
            where p.partner.id = :partnerId
            order by p.createdAt desc
            """,
            countQuery = """
            select count(p.id)
            from Proposal p
            where p.partner.id = :partnerId
            """)
    Page<OrgProposalRow> findOrgRows(@Param("partnerId") Long partnerId, Pageable pageable);
}
