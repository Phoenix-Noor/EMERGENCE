package com.emergence.core;

import java.util.Random;

/**
 * Deterministic pseudorandom number generator wrapper.
 * Ensures all simulation randomness is seeded and reproducible.
 */
public final class DeterministicRandom {

    private final long seed;
    private final Random random;

    public DeterministicRandom(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    public long getSeed() {
        return seed;
    }

    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public int nextInt(int origin, int bound) {
        return random.nextInt(origin, bound);
    }

    public double nextDouble() {
        return random.nextDouble();
    }

    public double nextDouble(double origin, double bound) {
        return random.nextDouble(origin, bound);
    }

    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    public double nextGaussian() {
        return random.nextGaussian();
    }
}
