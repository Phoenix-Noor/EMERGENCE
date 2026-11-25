package com.emergence.game;

/**
 * Built-in game-theoretic strategy types.
 */
public enum StrategyType {
    ALWAYS_COOPERATE("Always Cooperate"),
    ALWAYS_DEFECT("Always Defect"),
    TIT_FOR_TAT("Tit-for-Tat"),
    RANDOM("Random"),
    CONDITIONAL_COOPERATION("Conditional Cooperation"),
    RESOURCE_MAXIMIZER("Resource Maximizer");

    private final String displayName;

    StrategyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
