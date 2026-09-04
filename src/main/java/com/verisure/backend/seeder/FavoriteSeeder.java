package com.verisure.backend.seeder;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Favorite;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.FavoriteRepository;
import com.verisure.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Siembra 6 «me gusta» sobre las actividades visible en el catálogo.
 *
 * <p>Los pares se generan recorriendo dos bucles anidados, nunca al azar, para
 * respetar la restricción única {@code (activity_id, user_id)} de la tabla.
 *
 * <p>Dueña: BE3 · Tarea: B3-07
 */
@Component
@Order(6)
@Profile("!prod")
@RequiredArgsConstructor
public class FavoriteSeeder implements CommandLineRunner {

    private static final int HOW_MANY = 6;

    private final FavoriteRepository favoriteRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (favoriteRepository.count() > 0) {
            return; // idempotente: no duplica al reiniciar
        }

        List<User> employees = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .toList();

        // Solo las que se ven en el catálogo: nadie puede marcar un borrador.
        List<Activity> visible = activityRepository.findAll().stream()
                .filter(a -> a.getStatus() == ActivityStatus.PUBLISHED
                          || a.getStatus() == ActivityStatus.FULL)
                .toList();

        List<Favorite> favorites = new ArrayList<>();
        int created = 0;
        for (int i = 0; i < employees.size() && created < HOW_MANY; i++) {
            for (int j = 0; j < visible.size() && created < HOW_MANY; j++) {
                // El desplazamiento reparte los favorites entre actividades distintas
                // en vez de amontonarlos todos en la primera.
                if ((i + j) % 2 != 0) {
                    continue;
                }
                Favorite f = new Favorite();
                f.setUser(employees.get(i));
                f.setActivity(visible.get(j));
                favorites.add(f);
                created++;
            }
        }

        favoriteRepository.saveAll(favorites);
    }
}
