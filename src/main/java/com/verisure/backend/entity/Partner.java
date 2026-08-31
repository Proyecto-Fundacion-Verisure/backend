package com.verisure.backend.entity;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.verisure.backend.entity.enums.PartnerStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "partners")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(unique = true, nullable = false, length = 9)
    private String cif;

    @Column(name = "contact_name", length = 120)
    private String contactName;
    
    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartnerStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "partner")
    private List<User> users;

    @JsonIgnore
    @OneToMany(mappedBy = "partner")
    private List<Activity> activities;

    @JsonIgnore
    @OneToMany(mappedBy = "partner")
    private List<Proposal> proposals;
}
