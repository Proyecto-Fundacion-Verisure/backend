package com.verisure.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.verisure.backend.entity.Partner;

public interface PartnerRepository extends JpaRepository<Partner, Long> {

    /** Lo usa el registro: el CIF decide si la entidad ya existe. */
    Optional<Partner> findByCif(String cif);
}
