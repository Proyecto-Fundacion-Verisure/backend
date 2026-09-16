package com.verisure.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verisure.backend.dto.orgaccount.OrgAccountRow;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.entity.enums.UserStatus;
import com.verisure.backend.repository.projection.UserMailView;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Lo usa el login: el correo es el identificador con el que se entra. */
    Optional<User> findByEmail(String email);

    /** El enlace de verificación apunta a la única cuenta que lo tiene · {@code B1-15}. */
    Optional<User> findByVerificationToken(String token);

    /** ¿Alguna cuenta usa ya ese correo? El email es único por persona. */
    boolean existsByEmail(String email);

    /** ¿Esa persona ya tiene cuenta en esa entidad? */
    boolean existsByEmailAndPartnerId(String email, Long partnerId);

    /** Nombre y correo de una cuenta, para sus avisos · {@code B3-09}. */
    @Query("""
            select new com.verisure.backend.repository.projection.UserMailView(
                u.fullName, u.email, u.verificationToken, u.verificationTokenCreatedAt)
            from User u where u.id = :userId
            """)
    Optional<UserMailView> findMailViewById(@Param("userId") Long userId);

    /** Los destinatarios de un rol. La usa el aviso a administración · {@code B3-09}. */
    @Query("""
            select new com.verisure.backend.repository.projection.UserMailView(
                u.fullName, u.email, u.verificationToken, u.verificationTokenCreatedAt)
            from User u where u.role = :role
            """)
    List<UserMailView> findMailViewsByRole(@Param("role") Role role);

    /**
     * Fila de administración de una cuenta de entidad · {@code B1-16}.
     *
     * <p>El {@code requestedAt} es cuándo se pidió el alta: {@code createdAt} de
     * la persona si lo hay (registro), si no la creación de la entidad.
     */
    @Query(value = """
            select new com.verisure.backend.dto.orgaccount.OrgAccountRow(
                u.id, p.name, p.cif, u.fullName, u.email, p.phone,
                coalesce(u.createdAt, p.createdAt), u.status,
                case when u.verifiedAt is null then false else true end)
            from User u
            left join u.partner p
            where u.role = com.verisure.backend.entity.enums.Role.PARTNER
              and u.status in :statuses
            order by coalesce(u.createdAt, p.createdAt) asc
            """,
            countQuery = """
            select count(u.id)
            from User u
            where u.role = com.verisure.backend.entity.enums.Role.PARTNER
              and u.status in :statuses
            """)
    Page<OrgAccountRow> findAccounts(@Param("statuses") List<UserStatus> statuses, Pageable pageable);

    /** La usa el rechazo: no se baja la entidad si alguien dentro sigue activo · {@code B1-16}. */
    long countByPartnerIdAndStatus(Long partnerId, UserStatus status);

}
