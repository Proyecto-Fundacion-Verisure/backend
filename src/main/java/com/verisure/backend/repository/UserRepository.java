package com.verisure.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.verisure.backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Lo usa el login: el correo es el identificador con el que se entra. */
    Optional<User> findByEmail(String email);
}
