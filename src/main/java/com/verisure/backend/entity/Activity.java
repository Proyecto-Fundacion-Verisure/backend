package com.verisure.backend.entity;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.verisure.backend.entity.enums.ActivityStatus;

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

@Entity
@Table(name = "activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(length=160,nullable=false)
private String title;

@Column(nullable=false, columnDefinition = "text")
private String description;

@Column(length=60,nullable=false)
private String line;

@Column(length=20,nullable=false)
private String mode;

@Column(length=160)
private String location;

@Column(name="start_date",nullable=false)
private LocalDate startDate;

@Column(name="end_date",nullable=false)
private LocalDate endDate;

@Column(name="registration_deadline",nullable=false)
private LocalDate registrationDeadline;

@Column(nullable=false)
private Integer hours;

@Column(nullable=false)
private Integer spots;

@Column(name="image_url",length=255)
private String imageUrl;

@Column(nullable=false, length = 20)
@Enumerated(EnumType.STRING)
private ActivityStatus status;

@Column(name="review_note", columnDefinition = "text")
private String reviewNote;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parter_id", nullable = true, referencedColumnName = "id")
private Partner partner;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "created_by", nullable = true, referencedColumnName = "id")
private User createdBy;

@JsonIgnore
@OneToMany(mappedBy = "activity")
private List<Registration> registrations;

@JsonIgnore
@OneToMany(mappedBy = "activity")
private List<Favorite> favorites;

}
