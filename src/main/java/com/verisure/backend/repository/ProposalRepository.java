package com.verisure.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.verisure.backend.entity.Proposal;

public interface ProposalRepository extends JpaRepository<Proposal, Long>{

}
