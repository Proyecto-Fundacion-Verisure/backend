package com.verisure.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.entity.Favorite;
import com.verisure.backend.entity.User;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.FavoriteRepository;
import com.verisure.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void add(Long activityId, String userEmail) {
        requireActivityExists(activityId);
        User user = findUserOrFail(userEmail);

        boolean alreadyFavorited = favoriteRepository.existsByActivityIdAndUserId(
                activityId, user.getId());
        if (alreadyFavorited) {
            return;
        }

        Favorite favorite = new Favorite();
        // La actividad no se carga entera: nadie lee sus campos después.
        favorite.setActivity(activityRepository.getReferenceById(activityId));
        favorite.setUser(user);
        favoriteRepository.save(favorite);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void remove(Long activityId, String userEmail) {
        requireActivityExists(activityId);
        User user = findUserOrFail(userEmail);

        favoriteRepository.deleteByActivityIdAndUserId(activityId, user.getId());
    }

    private void requireActivityExists(Long activityId) {
        boolean activityMissing = !activityRepository.existsById(activityId);
        if (activityMissing) {
            throw NotFoundException.of("actividad", activityId);
        }
    }

    private User findUserOrFail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No existe la cuenta de " + email));
    }
}
