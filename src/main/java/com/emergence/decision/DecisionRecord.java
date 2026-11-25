package com.emergence.decision;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Explainable record of why an agent selected a specific action at a given tick.
 */
public record DecisionRecord(
        long tick,
        long agentId,
        String selectedAction,
        double selectedUtility,
        Map<String, Double> candidateUtilities,
        String observationSummary,
        String reason
) {
    public DecisionRecord {
        Objects.requireNonNull(selectedAction, "selectedAction must not be null");
        candidateUtilities = Collections.unmodifiableMap(new LinkedHashMap<>(candidateUtilities));
        observationSummary = observationSummary != null ? observationSummary : "";
        reason = reason != null ? reason : "";
    }
}
