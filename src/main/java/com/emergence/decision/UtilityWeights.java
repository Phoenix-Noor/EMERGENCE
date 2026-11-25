package com.emergence.decision;

/**
 * Configurable utility function weights:
 * U(a) = w1 * survival(a) + w2 * resources(a) + w3 * socialBenefit(a) - w4 * risk(a)
 */
public record UtilityWeights(
        double survivalWeight,
        double resourceWeight,
        double socialWeight,
        double riskWeight
) {
    public static final UtilityWeights DEFAULT = new UtilityWeights(1.5, 1.2, 0.8, 1.3);

    public UtilityWeights {
        if (survivalWeight < 0 || resourceWeight < 0 || socialWeight < 0 || riskWeight < 0) {
            throw new IllegalArgumentException("Utility weights cannot be negative");
        }
    }
}
