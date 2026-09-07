package com.aimmaster.repository;

import com.aimmaster.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    Optional<Room> findByCode(String code);
    boolean existsByCode(String code);
}
