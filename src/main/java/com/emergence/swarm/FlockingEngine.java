package com.emergence.swarm;

import com.emergence.agent.Agent;
import com.emergence.core.SimulationConfig;
import com.emergence.world.Position;

import java.util.List;

/**
 * Implements the three foundational flocking rules: Separation, Alignment, and Cohesion.
 */
public final class FlockingEngine {

    public record Vector2D(double x, double y) {
        public static final Vector2D ZERO = new Vector2D(0.0, 0.0);

        public double magnitude() {
            return Math.hypot(x, y);
        }

        public Vector2D normalize() {
            double mag = magnitude();
            if (mag < 1e-9) {
                return ZERO;
            }
            return new Vector2D(x / mag, y / mag);
        }

        public Vector2D scale(double factor) {
            return new Vector2D(x * factor, y * factor);
        }

        public Vector2D add(Vector2D other) {
            return new Vector2D(x + other.x, y + other.y);
        }
    }

    public Vector2D computeSeparation(Agent self, List<Agent> neighbours, int separationRadius) {
        double sx = 0.0;
        double sy = 0.0;
        int count = 0;

        for (Agent other : neighbours) {
            if (other.getId() == self.getId() || !other.isAlive()) {
                continue;
            }
            double dist = self.getPosition().euclideanDistanceTo(other.getPosition());
            if (dist > 0.0 && dist <= separationRadius) {
                double dx = self.getPosition().x() - other.getPosition().x();
                double dy = self.getPosition().y() - other.getPosition().y();
                sx += dx / dist;
                sy += dy / dist;
                count++;
            } else if (dist == 0.0) {
                // Deterministic tie-breaker push when sharing the exact same cell
                sx += (self.getId() % 2 == 0) ? 1.0 : -1.0;
                sy += (self.getId() % 3 == 0) ? 1.0 : -1.0;
                count++;
            }
        }
        return count == 0 ? Vector2D.ZERO : new Vector2D(sx / count, sy / count).normalize();
    }

    public Vector2D computeAlignment(Agent self, List<Agent> neighbours) {
        double ax = 0.0;
        double ay = 0.0;
        int count = 0;

        for (Agent other : neighbours) {
            if (other.getId() == self.getId() || !other.isAlive()) {
                continue;
            }
            if (other.getLastMoveDx() != 0 || other.getLastMoveDy() != 0) {
                ax += other.getLastMoveDx();
                ay += other.getLastMoveDy();
                count++;
            }
        }
        return count == 0 ? Vector2D.ZERO : new Vector2D(ax / count, ay / count).normalize();
    }

    public Vector2D computeCohesion(Agent self, List<Agent> neighbours) {
        double cx = 0.0;
        double cy = 0.0;
        int count = 0;

        for (Agent other : neighbours) {
            if (other.getId() == self.getId() || !other.isAlive()) {
                continue;
            }
            cx += other.getPosition().x();
            cy += other.getPosition().y();
            count++;
        }
        if (count == 0) {
            return Vector2D.ZERO;
        }
        double meanX = cx / count;
        double meanY = cy / count;
        return new Vector2D(meanX - self.getPosition().x(), meanY - self.getPosition().y()).normalize();
    }

    public Position computeFlockingStep(Agent self, List<Agent> neighbours, SimulationConfig config) {
        if (neighbours.isEmpty()) {
            return null;
        }
        Vector2D sep = computeSeparation(self, neighbours, config.getSeparationRadius())
                .scale(config.getSeparationWeight());
        Vector2D ali = computeAlignment(self, neighbours)
                .scale(config.getAlignmentWeight());
        Vector2D coh = computeCohesion(self, neighbours)
                .scale(config.getCohesionWeight() * (0.4 + self.getTraits().sociability()));

        Vector2D combined = sep.add(ali).add(coh);
        if (combined.magnitude() < 0.15) {
            return null;
        }
        int dx = (int) Math.round(Math.max(-1.0, Math.min(1.0, combined.x())));
        int dy = (int) Math.round(Math.max(-1.0, Math.min(1.0, combined.y())));
        if (dx == 0 && dy == 0) {
            return null;
        }
        return self.getPosition().translate(dx, dy);
    }
}
