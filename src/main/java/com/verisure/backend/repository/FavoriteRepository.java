package com.verisure.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.verisure.backend.entity.Favorite;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

}
