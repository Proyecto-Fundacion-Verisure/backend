package com.verisure.backend.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
@Table(name = "participation_closures")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationClosure {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="actual_hours",nullable=false)
    private Integer  actualHours;

    @Column(nullable=false)
    private Integer rating;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name="evidence_url",length=255)
    private String evidenceUrl;

    @Column(name="submitted_at",nullable=false)
    private Instant submittedAt;

    @OneToOne
    @JoinColumn(name="registration_id", nullable=false,unique=true, referencedColumnName="id")
    private Registration registration;

}
