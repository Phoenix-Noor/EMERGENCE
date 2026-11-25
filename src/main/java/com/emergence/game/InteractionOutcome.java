package com.emergence.game;

/**
 * Immutable record of a single repeated-game interaction from one agent's perspective.
 */
public record InteractionOutcome(
        long tick,
        long partnerId,
        GameMove selfMove,
        GameMove partnerMove,
        double selfPayoff,
        double partnerPayoff
) {
}
