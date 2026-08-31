package com.verisure.backend.entity;

import java.time.Instant;

import com.verisure.backend.entity.enums.ActivityClosureStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_closures")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityClosure {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collaboration_rating")
    private Integer collaborationRating;   

    @Column(name = "closing_notes", columnDefinition = "text")
    private String closingNotes;

    @Column(name = "lessons_learned", columnDefinition = "text")
    private String lessonsLearned;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityClosureStatus status;

    @Column(name = "closed_at")
    private Instant closedAt;         
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false, unique = true)
    private Activity activity;
}
