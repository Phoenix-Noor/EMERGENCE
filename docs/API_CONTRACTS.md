# EMERGENCE — API Contracts

## Core Interfaces

- `SimulationModule`: `initialize(SimulationContext)`, `update(SimulationContext)`, `shutdown()`.
- `Action`: `getName()`, `isValid(SimulationContext)`, `execute(SimulationContext)`.
- `AgentStrategy`: `getStrategyName()`, `chooseAction(Agent, Perception, SimulationContext)`.
- `Experiment`: `getName()`, `getDescription()`.

## Shared Data Invariants
- `Agent.id` is strictly positive and unique per simulation run.
- `Cell.resourceAmount` is always in `[0.0, maxResource]`.
- `Agent.energy` is in `[0.0, maxEnergy]` and `Agent.health` is in `[0.0, maxHealth]`.
- Dead agents (`!agent.isAlive()`) are removed at the end of each tick and never execute actions.
