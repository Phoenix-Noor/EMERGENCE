package com.emergence.swarm;

import com.emergence.world.Position;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Leaderless dynamic group of nearby agents.
 */
public final class AgentGroup {

    private final long id;
    private final Set<Long> memberIds = new LinkedHashSet<>();
    private Position centroid;
    private double cohesion;

    public AgentGroup(long id, Position initialCentroid) {
        if (id <= 0) {
            throw new IllegalArgumentException("Group id must be positive");
        }
        this.id = id;
        this.centroid = Objects.requireNonNull(initialCentroid, "initialCentroid must not be null");
        this.cohesion = 1.0;
    }

    public long getId() { return id; }

    public Set<Long> getMemberIds() {
        return Collections.unmodifiableSet(memberIds);
    }

    public int size() {
        return memberIds.size();
    }

    public Position getCentroid() { return centroid; }
    public void setCentroid(Position centroid) {
        this.centroid = Objects.requireNonNull(centroid, "centroid must not be null");
    }

    public double getCohesion() { return cohesion; }
    public void setCohesion(double cohesion) {
        this.cohesion = Math.max(0.0, Math.min(1.0, cohesion));
    }

    public void addMember(long agentId) {
        memberIds.add(agentId);
    }

    public void removeMember(long agentId) {
        memberIds.remove(agentId);
    }

    public boolean contains(long agentId) {
        return memberIds.contains(agentId);
    }
}
