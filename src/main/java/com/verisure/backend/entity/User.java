package com.verisure.backend.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.verisure.backend.entity.enums.Organization;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.entity.enums.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor

public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private Organization organization;

    @Column(nullable = true, length = 80)
    private String department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = true, referencedColumnName = "id")
    private Partner partner;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    /** Vigencia del enlace de verificación · {@code B1-15}. */
    public static final Duration VERIFICATION_TTL = Duration.ofHours(24);

    /** Token aleatorio del enlace de verificación · {@code B1-15}. */
    @Column(nullable = true, length = 64)
    private String verificationToken;

    /** Cuándo se generó el token, para hacerlo caducar a las 24 horas. */
    @Column(nullable = true)
    private Instant verificationTokenCreatedAt;

    /** Cuándo se verificó el correo. Nulo mientras no se confirme; sirve de marca de usado. */
    @Column(nullable = true)
    private Instant verifiedAt;

    /** Fecha de solicitud del alta: la «requestedAt» de la vista de administración. */
    @Column(nullable = true)
    private Instant createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    private List<Registration> registrations;

    @JsonIgnore
    @OneToMany(mappedBy = "decidedBy")
    private List<Registration> decidedRegistrations;

    @JsonIgnore
@OneToMany(mappedBy = "createdBy")
private List<Activity> activitiesCreated;

@JsonIgnore
@OneToMany(mappedBy = "user")
private List<Favorite> favorites;
}
