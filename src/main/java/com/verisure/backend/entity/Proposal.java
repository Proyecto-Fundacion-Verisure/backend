package com.verisure.backend.entity;

import java.time.Instant;

import com.verisure.backend.entity.enums.ProposalStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

@Entity
@Table(name = "proposals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Proposal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, columnDefinition = "text")
    private String description;

    @Column(name="estimated_volunteers")
    private int estimatedVolunteers;

    @Column(name="suggested_line", length=60)
    private String suggestedLine;

    @Column(length=120)
    private String scope;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProposalStatus status;

    @Column(name = "consent_at", nullable = false)
    private Instant  consentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = true, referencedColumnName = "id")
    private Partner partner;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = true, referencedColumnName = "id")
    private Activity activity;
}
