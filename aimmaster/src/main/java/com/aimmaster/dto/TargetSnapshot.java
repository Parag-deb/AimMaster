package com.aimmaster.dto;

import com.aimmaster.model.Target;
import com.aimmaster.model.TargetType;

/**
 * A read-only, single-moment-in-time copy of one Target, safe to serialize to JSON
 * and send to clients. We never send the actual Target object over the wire - clients
 * get a snapshot, the server keeps the only mutable copy (server-authoritative state,
 * per FR13).
 */
public class TargetSnapshot {

    private final String id;
    private final TargetType type;
    private final double x;
    private final double y;

    public TargetSnapshot(String id, TargetType type, double x, double y) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public static TargetSnapshot from(Target target) {
        return new TargetSnapshot(target.getId(), target.getType(), target.getX(), target.getY());
    }

    public String getId() {
        return id;
    }

    public TargetType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
