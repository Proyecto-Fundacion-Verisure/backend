package com.verisure.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.repository.projection.UserMailView;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Lo usa el login: el correo es el identificador con el que se entra. */
    Optional<User> findByEmail(String email);

    /** ¿Alguna cuenta usa ya ese correo? El email es único por persona. */
    boolean existsByEmail(String email);

    /** ¿Esa persona ya tiene cuenta en esa entidad? */
    boolean existsByEmailAndPartnerId(String email, Long partnerId);

    /** Nombre y correo de una cuenta, para sus avisos · {@code B3-09}. */
    @Query("""
            select new com.verisure.backend.repository.projection.UserMailView(u.fullName, u.email)
            from User u where u.id = :userId
            """)
    Optional<UserMailView> findMailViewById(@Param("userId") Long userId);

    /** Los destinatarios de un rol. La usa el aviso a administración · {@code B3-09}. */
    @Query("""
            select new com.verisure.backend.repository.projection.UserMailView(u.fullName, u.email)
            from User u where u.role = :role
            """)
    List<UserMailView> findMailViewsByRole(@Param("role") Role role);

}
