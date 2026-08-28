package com.verisure.backend.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
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
    @JoinColumn(name = "parter_id", nullable = true, referencedColumnName = "id")
    private Partner partner;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;
}
