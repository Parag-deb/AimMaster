package com.aimmaster.repository;

import com.aimmaster.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Handles saving/loading Player rows. We just extend JpaRepository and Spring
 * generates the implementation for us at startup - no SQL to write by hand.
 */
public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByUsername(String username);

    boolean existsByUsername(String username);
}
