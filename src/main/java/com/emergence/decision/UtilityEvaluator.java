package com.emergence.decision;

import com.emergence.agent.Agent;
import com.emergence.agent.Perception;
import com.emergence.core.SimulationContext;
import com.emergence.world.Cell;
import com.emergence.world.Terrain;

import java.util.Objects;

/**
 * Evaluates candidate actions using:
 * U(a) = w1 * survival(a) + w2 * resources(a) + w3 * socialBenefit(a) - w4 * risk(a)
 */
public final class UtilityEvaluator {

    private final UtilityWeights baseWeights;

    public UtilityEvaluator() {
        this(UtilityWeights.DEFAULT);
    }

    public UtilityEvaluator(UtilityWeights baseWeights) {
        this.baseWeights = Objects.requireNonNull(baseWeights, "baseWeights must not be null");
    }

    public record Evaluation(double totalUtility, double survival, double resources, double social, double risk, String reason) {
    }

    public Evaluation evaluate(Action action, Agent agent, Perception perception, SimulationContext context) {
        double energyRatio = agent.getEnergy() / agent.getMaxEnergy();
        double healthRatio = agent.getHealth() / agent.getMaxHealth();

        // Modulate weights slightly by agent traits
        double wSurvival = baseWeights.survivalWeight() * (1.0 + (1.0 - energyRatio));
        double wResource = baseWeights.resourceWeight() * (1.0 + (1.0 - energyRatio) * 0.5);
        double wSocial = baseWeights.socialWeight() * (0.5 + agent.getTraits().sociability());
        double wRisk = baseWeights.riskWeight() * (1.5 - agent.getTraits().riskTolerance());

        double survival = 0.0;
        double resources = 0.0;
        double social = 0.0;
        double risk = 0.0;
        String reason;

        if (action instanceof ReproduceAction) {
            survival = 0.4 * healthRatio;
            resources = -0.3; // costs energy
            social = 0.9 * (0.5 + agent.getTraits().cooperation());
            risk = 0.25 * (1.0 - energyRatio);
            reason = "Healthy & high energy; reproduction increases lineage";
        } else if (action instanceof ConsumeResourceAction) {
            double avail = Math.min(1.0, perception.getCurrentCell().getResourceAmount() / context.getConfig().getFoodConsumptionAmount());
            survival = (1.0 - energyRatio) * 1.2 + 0.2;
            resources = avail;
            social = 0.0;
            risk = perception.getCurrentCell().getTerrain() == Terrain.HAZARD ? 0.5 : 0.0;
            reason = "Food available on current cell";
        } else if (action instanceof MoveAction move) {
            Cell target = context.getWorld().getCell(context.getWorld().normalizePosition(move.getTargetPosition()));
            double foodScore = Math.min(1.0, target.getResourceAmount() / context.getConfig().getMaxCellResource());
            boolean hasVisibleFood = perception.getVisibleCells().stream().anyMatch(c -> c.getResourceAmount() > 0.5);

            survival = (energyRatio < 0.4 && hasVisibleFood) ? 0.55 : 0.15;
            resources = foodScore * 0.9 + (hasVisibleFood ? 0.25 : 0.1 * agent.getTraits().exploration());
            long nearbyCount = perception.getNearbyAgents().size();
            social = (nearbyCount > 0 ? 0.35 : 0.1) * agent.getTraits().sociability();
            risk = target.getTerrain() == Terrain.HAZARD ? 1.0 : 0.05 * target.getTerrain().getMovementCostMultiplier();
            reason = target.getResourceAmount() > 0.5 ? "Moving toward food" : "Exploring passable terrain";
        } else {
            // RestAction or fallback
            survival = (1.0 - healthRatio) * 0.7 + 0.1;
            resources = 0.0;
            social = 0.05;
            risk = perception.getCurrentCell().getTerrain() == Terrain.HAZARD ? 0.8 : 0.0;
            reason = "Resting to conserve energy and recover health";
        }

        double total = wSurvival * survival + wResource * resources + wSocial * social - wRisk * risk;
        return new Evaluation(total, survival, resources, social, risk, reason);
    }
}
