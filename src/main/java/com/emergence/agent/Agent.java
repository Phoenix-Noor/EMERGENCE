package com.emergence.agent;

import com.emergence.decision.DecisionRecord;
import com.emergence.game.StrategyType;
import com.emergence.world.Position;

import java.util.Objects;

/**
 * Autonomous participant in the artificial world.
 */
public final class Agent {

    private final long id;
    private final int generation;
    private final int perceptionRadius;
    private final double maxEnergy;
    private final double maxHealth;
    private final AgentTraits traits;
    private final AgentMemory memory;

    private Position position;
    private double energy;
    private double health;
    private int age;
    private long parentId = 0L;
    private int offspringCount = 0;
    private int reproductionCooldown;

    public long getParentId() { return parentId; }
    public void setParentId(long parentId) { this.parentId = parentId; }

    public int getOffspringCount() { return offspringCount; }
    public void incrementOffspringCount() { this.offspringCount++; }
    private AgentState state;
    private String lastActionName = "NONE";
    private StrategyType strategyType = StrategyType.TIT_FOR_TAT;
    private DecisionRecord lastDecision;
    private long groupId = 0L;
    private int lastMoveDx = 0;
    private int lastMoveDy = 0;
    private boolean signalActive = false;

    public Agent(
            long id,
            Position position,
            double initialEnergy,
            double maxEnergy,
            double initialHealth,
            double maxHealth,
            int perceptionRadius,
            int generation,
            AgentTraits traits
    ) {
        if (id <= 0) {
            throw new IllegalArgumentException("Agent id must be positive");
        }
        if (maxEnergy <= 0 || maxHealth <= 0) {
            throw new IllegalArgumentException("maxEnergy and maxHealth must be positive");
        }
        if (perceptionRadius < 0) {
            throw new IllegalArgumentException("perceptionRadius cannot be negative");
        }
        this.id = id;
        this.position = Objects.requireNonNull(position, "position must not be null");
        this.maxEnergy = maxEnergy;
        this.maxHealth = maxHealth;
        this.energy = clamp(initialEnergy, 0.0, maxEnergy);
        this.health = clamp(initialHealth, 0.0, maxHealth);
        this.perceptionRadius = perceptionRadius;
        this.generation = generation;
        this.traits = Objects.requireNonNull(traits, "traits must not be null");
        this.memory = new AgentMemory();
        this.age = 0;
        this.reproductionCooldown = 0;
        this.state = (this.energy > 0 && this.health > 0) ? AgentState.ACTIVE : AgentState.DEAD;
    }

    public long getId() { return id; }
    public int getGeneration() { return generation; }
    public int getPerceptionRadius() { return perceptionRadius; }
    public double getMaxEnergy() { return maxEnergy; }
    public double getMaxHealth() { return maxHealth; }
    public AgentTraits getTraits() { return traits; }
    public AgentMemory getMemory() { return memory; }

    public Position getPosition() { return position; }
    public void setPosition(Position position) {
        this.position = Objects.requireNonNull(position, "position must not be null");
    }

    public double getEnergy() { return energy; }
    public double getHealth() { return health; }
    public int getAge() { return age; }
    public int getReproductionCooldown() { return reproductionCooldown; }
    public AgentState getState() { return state; }
    public boolean isAlive() { return state != AgentState.DEAD; }
    public String getLastActionName() { return lastActionName; }

    public StrategyType getStrategyType() { return strategyType; }
    public void setStrategyType(StrategyType strategyType) {
        this.strategyType = Objects.requireNonNull(strategyType, "strategyType must not be null");
    }

    public DecisionRecord getLastDecision() { return lastDecision; }
    public void setLastDecision(DecisionRecord lastDecision) { this.lastDecision = lastDecision; }

    public long getGroupId() { return groupId; }
    public void setGroupId(long groupId) { this.groupId = groupId; }

    public int getLastMoveDx() { return lastMoveDx; }
    public int getLastMoveDy() { return lastMoveDy; }
    public void setLastMove(int dx, int dy) {
        this.lastMoveDx = dx;
        this.lastMoveDy = dy;
    }

    public boolean isSignalActive() { return signalActive; }
    public void setSignalActive(boolean signalActive) { this.signalActive = signalActive; }

    public void setLastActionName(String lastActionName) {
        this.lastActionName = lastActionName != null ? lastActionName : "NONE";
    }

    public void setState(AgentState state) {
        if (this.state == AgentState.DEAD && state != AgentState.DEAD) {
            return;
        }
        this.state = Objects.requireNonNull(state, "state must not be null");
    }

    public void adjustEnergy(double delta) {
        if (!isAlive()) {
            return;
        }
        this.energy = clamp(this.energy + delta, 0.0, maxEnergy);
        if (this.energy <= 0.0) {
            this.state = AgentState.DEAD;
        }
    }

    public void adjustHealth(double delta) {
        if (!isAlive()) {
            return;
        }
        this.health = clamp(this.health + delta, 0.0, maxHealth);
        if (this.health <= 0.0) {
            this.state = AgentState.DEAD;
        }
    }

    public void incrementAge() {
        if (isAlive()) {
            age++;
            if (reproductionCooldown > 0) {
                reproductionCooldown--;
            }
        }
    }

    public void setReproductionCooldown(int cooldownTicks) {
        this.reproductionCooldown = Math.max(0, cooldownTicks);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
