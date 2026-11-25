package com.emergence.ecosystem;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentTraits;
import com.emergence.core.SimulationContext;
import com.emergence.core.SimulationModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks multi-generation evolution, population-level trait distributions, diversity, and fitness metrics.
 */
public final class EvolutionEngine implements SimulationModule {

    private static final int MAX_HISTORY = 300;

    public record TraitDistributionSnapshot(
            long tick,
            int maxGeneration,
            double averageGeneration,
            double meanAggression,
            double meanCooperation,
            double meanSociability,
            double meanExploration,
            double meanRiskTolerance,
            double meanTrustPropensity,
            double traitDiversity
    ) {
    }

    private int maxGeneration = 1;
    private double averageGeneration = 1.0;
    private AgentTraits meanTraits = new AgentTraits(0.5, 0.5, 0.5, 0.5, 0.5, 0.5);
    private double traitDiversity = 0.0;
    private double averageOffspringCount = 0.0;
    private final List<TraitDistributionSnapshot> history = new ArrayList<>();

    @Override
    public void initialize(SimulationContext context) {
        history.clear();
        update(context);
    }

    @Override
    public void update(SimulationContext context) {
        List<Agent> living = new ArrayList<>();
        for (Agent a : context.getAgents()) {
            if (a.isAlive()) {
                living.add(a);
            }
        }
        if (living.isEmpty()) {
            return;
        }

        int maxGen = 1;
        double sumGen = 0.0;
        double sumOffspring = 0.0;
        double sAgg = 0, sCoop = 0, sSoc = 0, sExp = 0, sRisk = 0, sTrust = 0;

        for (Agent a : living) {
            if (a.getGeneration() > maxGen) {
                maxGen = a.getGeneration();
            }
            sumGen += a.getGeneration();
            sumOffspring += a.getOffspringCount();
            AgentTraits t = a.getTraits();
            sAgg += t.aggression();
            sCoop += t.cooperation();
            sSoc += t.sociability();
            sExp += t.exploration();
            sRisk += t.riskTolerance();
            sTrust += t.trustPropensity();
        }

        int n = living.size();
        this.maxGeneration = maxGen;
        this.averageGeneration = sumGen / n;
        this.averageOffspringCount = sumOffspring / n;
        this.meanTraits = new AgentTraits(
                sAgg / n, sCoop / n, sSoc / n, sExp / n, sRisk / n, sTrust / n
        );

        // Compute mean standard deviation across the 6 traits as population diversity
        double vAgg = 0, vCoop = 0, vSoc = 0, vExp = 0, vRisk = 0, vTrust = 0;
        for (Agent a : living) {
            AgentTraits t = a.getTraits();
            vAgg += sq(t.aggression() - meanTraits.aggression());
            vCoop += sq(t.cooperation() - meanTraits.cooperation());
            vSoc += sq(t.sociability() - meanTraits.sociability());
            vExp += sq(t.exploration() - meanTraits.exploration());
            vRisk += sq(t.riskTolerance() - meanTraits.riskTolerance());
            vTrust += sq(t.trustPropensity() - meanTraits.trustPropensity());
        }
        this.traitDiversity = (Math.sqrt(vAgg / n) + Math.sqrt(vCoop / n) + Math.sqrt(vSoc / n)
                + Math.sqrt(vExp / n) + Math.sqrt(vRisk / n) + Math.sqrt(vTrust / n)) / 6.0;

        history.add(new TraitDistributionSnapshot(
                context.getClock().getCurrentTick(),
                maxGeneration,
                averageGeneration,
                meanTraits.aggression(),
                meanTraits.cooperation(),
                meanTraits.sociability(),
                meanTraits.exploration(),
                meanTraits.riskTolerance(),
                meanTraits.trustPropensity(),
                traitDiversity
        ));
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    private static double sq(double d) {
        return d * d;
    }

    public int getMaxGeneration() { return maxGeneration; }
    public double getAverageGeneration() { return averageGeneration; }
    public AgentTraits getMeanTraits() { return meanTraits; }
    public double getTraitDiversity() { return traitDiversity; }
    public double getAverageOffspringCount() { return averageOffspringCount; }

    public List<TraitDistributionSnapshot> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
