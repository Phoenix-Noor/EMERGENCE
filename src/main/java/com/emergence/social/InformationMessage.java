package com.emergence.social;

import com.emergence.world.Position;

/**
 * Immutable information message transmitted across social relationships.
 */
public record InformationMessage(
        long id,
        long sourceAgentId,
        long creationTick,
        long expirationTick,
        InformationType type,
        Position position,
        String payload,
        double confidence
) {
    public enum InformationType {
        RESOURCE_LOCATION,
        HAZARD_LOCATION,
        BEHAVIOURAL_RECOMMENDATION
    }

    public boolean isExpired(long currentTick) {
        return currentTick > expirationTick;
    }
}
