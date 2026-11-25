package com.emergence.world;

import java.util.Objects;

/**
 * Immutable 2D grid coordinate in the simulation world.
 */
public record Position(int x, int y) {

    public int manhattanDistanceTo(Position other) {
        Objects.requireNonNull(other, "other position must not be null");
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    public double euclideanDistanceTo(Position other) {
        Objects.requireNonNull(other, "other position must not be null");
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.hypot(dx, dy);
    }

    public Position translate(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }
}
