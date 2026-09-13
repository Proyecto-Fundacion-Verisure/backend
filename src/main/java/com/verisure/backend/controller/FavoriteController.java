package com.verisure.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.favorite.FavoriteRequest;
import com.verisure.backend.service.FavoriteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * «Me gusta» del empleado sobre una actividad.
 *
 * <p>Ninguna respuesta lleva el recuento. En el catálogo y en la ficha el
 * corazón solo aparece marcado o sin marcar, para que un número bajo no
 * condicione a quien mira la actividad.
 *
 * <p>Dueña: BE3 · Tarea: B3-07.
 */
@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** Marca una actividad como favorita. */
    @PostMapping("/api/favorites")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Void> add(
            @Valid @RequestBody FavoriteRequest request,
            Authentication authentication) {

        favoriteService.add(request.activityId(), authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Desmarca una actividad, estuviera marcada o no. */
    @DeleteMapping("/api/favorites/{activityId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Void> remove(
            @PathVariable Long activityId,
            Authentication authentication) {

        favoriteService.remove(activityId, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
