package com.emergence.game;

/**
 * Configurable 2x2 symmetric Prisoner's Dilemma payoff matrix.
 * Standard defaults: T=5 (temptation), R=3 (reward), P=1 (punishment), S=0 (sucker).
 */
public final class PayoffMatrix {

    private final double rewardBothCooperate;
    private final double suckerCooperatorVsDefector;
    private final double temptationDefectorVsCooperator;
    private final double punishmentBothDefect;

    public PayoffMatrix() {
        this(3.0, 0.0, 5.0, 1.0);
    }

    public PayoffMatrix(
            double rewardBothCooperate,
            double suckerCooperatorVsDefector,
            double temptationDefectorVsCooperator,
            double punishmentBothDefect
    ) {
        this.rewardBothCooperate = rewardBothCooperate;
        this.suckerCooperatorVsDefector = suckerCooperatorVsDefector;
        this.temptationDefectorVsCooperator = temptationDefectorVsCooperator;
        this.punishmentBothDefect = punishmentBothDefect;
    }

    public double payoffFor(GameMove selfMove, GameMove opponentMove) {
        if (selfMove == GameMove.COOPERATE && opponentMove == GameMove.COOPERATE) {
            return rewardBothCooperate;
        }
        if (selfMove == GameMove.COOPERATE && opponentMove == GameMove.DEFECT) {
            return suckerCooperatorVsDefector;
        }
        if (selfMove == GameMove.DEFECT && opponentMove == GameMove.COOPERATE) {
            return temptationDefectorVsCooperator;
        }
        return punishmentBothDefect;
    }

    public boolean isValidPrisonersDilemma() {
        return temptationDefectorVsCooperator > rewardBothCooperate
                && rewardBothCooperate > punishmentBothDefect
                && punishmentBothDefect > suckerCooperatorVsDefector
                && (2.0 * rewardBothCooperate) > (temptationDefectorVsCooperator + suckerCooperatorVsDefector);
    }

    public double getRewardBothCooperate() { return rewardBothCooperate; }
    public double getSuckerCooperatorVsDefector() { return suckerCooperatorVsDefector; }
    public double getTemptationDefectorVsCooperator() { return temptationDefectorVsCooperator; }
    public double getPunishmentBothDefect() { return punishmentBothDefect; }
}
