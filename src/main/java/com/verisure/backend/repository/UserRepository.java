package com.verisure.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.verisure.backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
