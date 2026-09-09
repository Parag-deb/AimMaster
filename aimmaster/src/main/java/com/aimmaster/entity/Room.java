package com.aimmaster.entity;

import com.aimmaster.model.Target;
import com.aimmaster.model.TargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 10)
    private String code;

    @ManyToOne
    @JoinColumn(name = "host_id", nullable = false)
    private Player host;

    @Embedded
    private RoomSettings settings;

    @Transient
    private final List<Player> players = new ArrayList<>();

    @Transient
    private final List<Target> targets = new ArrayList<>();

    protected Room() {
    }

    public Room(String code, Player host, RoomSettings settings) {
        this.code = code;
        this.host = Objects.requireNonNull(host);
        this.settings = Objects.requireNonNull(settings);
        this.players.add(host);
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Player getHost() {
        return host;
    }

    public RoomSettings getSettings() {
        return settings;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public List<Target> getTargets() {
        return Collections.unmodifiableList(targets);
    }

    public boolean addPlayer(Player player) {
        if (players.size() >= settings.getMaxPlayers()) {
            return false;
        }
        if (players.contains(player)) {
            return false;
        }
        players.add(player);
        return true;
    }

    public boolean removePlayer(Player player) {
        boolean removed = players.remove(player);
        return removed;
    }

    public void addTarget(Target target) {
        targets.add(target);
    }

    public void removeTarget(Target target) {
        targets.remove(target);
    }

    public void clearTargets() {
        targets.clear();
    }
}
