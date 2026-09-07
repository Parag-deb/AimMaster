package com.aimmaster.controller;

import com.aimmaster.dto.CreateRoomRequest;
import com.aimmaster.dto.JoinRoomRequest;
import com.aimmaster.dto.RoomResponse;
import com.aimmaster.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<RoomResponse> joinRoom(@PathVariable String code) {
        JoinRoomRequest request = new JoinRoomRequest(code);
        RoomResponse response = roomService.joinRoom(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> listRooms() {
        List<RoomResponse> rooms = roomService.listRooms();
        return ResponseEntity.ok(rooms);
    }
}
