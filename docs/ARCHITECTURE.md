# EMERGENCE — Architecture

## Architectural Principles
1. **UI Independence:** `com.emergence.core`, `world`, `agent`, `decision`, `game`, `swarm`, `social`, `ecosystem`, and `analytics` have zero dependency on JavaFX.
2. **Deterministic Execution:** All randomness flows through `DeterministicRandom` seeded from `SimulationConfig.randomSeed`.
3. **Consistent Snapshot Decisions:** Agents perceive the world state and choose actions before actions are resolved, preventing iteration-order bias.
4. **Single-Threaded Simulation Core:** The simulation mutates state on a single thread; JavaFX renders safely via synchronized access and `Platform.runLater`.

## Tick Execution Order
1. Advance `SimulationClock` tick counter.
2. Regenerate world resources (`World.regenerateResources()`).
3. Collect living agents sorted deterministically by `Agent.id`.
4. Generate `Perception` and select `Action` for each agent.
5. Validate and execute planned actions in deterministic order.
6. Apply base metabolism, age increment, and hazard damage.
7. Commit pending offspring and remove dead agents.
8. Update registered `SimulationModule` instances.
