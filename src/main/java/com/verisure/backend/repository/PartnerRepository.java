package com.verisure.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.verisure.backend.entity.Partner;

public interface PartnerRepository extends JpaRepository<Partner, Long> {

}
