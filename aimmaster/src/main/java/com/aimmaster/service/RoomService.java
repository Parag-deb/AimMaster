package com.aimmaster.service;

import com.aimmaster.dto.CreateRoomRequest;
import com.aimmaster.dto.JoinRoomRequest;
import com.aimmaster.dto.RoomResponse;
import com.aimmaster.entity.Player;
import com.aimmaster.entity.Room;
import com.aimmaster.entity.RoomSettings;
import com.aimmaster.exception.BadRequestException;
import com.aimmaster.exception.NotFoundException;
import com.aimmaster.repository.RoomRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public RoomResponse createRoom(CreateRoomRequest request) {
        Player host = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (roomRepository.existsByCode(request.code())) {
            throw new BadRequestException("Room code already exists");
        }

        RoomSettings settings = new RoomSettings(
                request.difficulty(),
                request.durationSeconds(),
                request.maxPlayers()
        );

        Room room = new Room(request.code(), host, settings);
        Room saved = roomRepository.save(room);

        return RoomResponse.from(saved, 1);
    }

    public RoomResponse joinRoom(JoinRoomRequest request) {
        Player player = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Room room = roomRepository.findByCode(request.code())
                .orElseThrow(() -> new NotFoundException("Room not found"));

        if (!room.addPlayer(player)) {
            throw new BadRequestException("Cannot join room: full or already inside");
        }

        Room saved = roomRepository.save(room);
        return RoomResponse.from(saved, room.getPlayers().size());
    }

    public List<RoomResponse> listRooms() {
        List<Room> rooms = roomRepository.findAll();
        List<RoomResponse> responses = new ArrayList<>();
        for (Room room : rooms) {
            responses.add(RoomResponse.from(room, room.getPlayers().size()));
        }
        return responses;
    }
}
