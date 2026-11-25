package com.emergence.social;

/**
 * Directed social relationship between two agents with bounded trust and strength in [0.0, 1.0].
 */
public final class Relationship {

    private final long sourceId;
    private final long targetId;
    private double trust;
    private double strength;
    private int interactionCount;
    private long lastInteractionTick;

    public Relationship(long sourceId, long targetId, double initialTrust, double initialStrength, long tick) {
        if (sourceId <= 0 || targetId <= 0 || sourceId == targetId) {
            throw new IllegalArgumentException("Invalid relationship endpoints");
        }
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.trust = clamp(initialTrust);
        this.strength = clamp(initialStrength);
        this.interactionCount = 1;
        this.lastInteractionTick = tick;
    }

    public long getSourceId() { return sourceId; }
    public long getTargetId() { return targetId; }
    public double getTrust() { return trust; }
    public double getStrength() { return strength; }
    public int getInteractionCount() { return interactionCount; }
    public long getLastInteractionTick() { return lastInteractionTick; }

    public void update(double trustDelta, double strengthDelta, long tick) {
        this.trust = clamp(this.trust + trustDelta);
        this.strength = clamp(this.strength + strengthDelta);
        this.interactionCount++;
        this.lastInteractionTick = tick;
    }

    public void decay(double strengthDecay) {
        this.strength = clamp(this.strength - strengthDecay);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
