package proctoring.strategy.risk;

import proctoring.strategy.external.ThreatIntelligenceFeed;
import proctoring.strategy.model.ExamDeliveryMode;

public class RiskStrategySelector {
    private final ThreatIntelligenceFeed threatFeed;

    public RiskStrategySelector(ThreatIntelligenceFeed threatFeed) {
        this.threatFeed = threatFeed;
    }

    public RiskScoringStrategy selectStrategy(ExamDeliveryMode mode, int studentRiskTier) {
        if (mode == null) {
            return new ProcessKeystrokeRiskStrategy();
        }

        switch (mode) {
            case REMOTE_HIGH_STAKES:
                if (studentRiskTier >= 3) {
                    return new NetworkThreatAdaptiveRiskStrategy(threatFeed);
                }
                return new StrictBiometricRiskStrategy();

            case REMOTE_UNSUPERVISED:
                return new NetworkThreatAdaptiveRiskStrategy(threatFeed);

            case CAMPUS_LAB_MONITORED:
                return new ProcessKeystrokeRiskStrategy();

            default:
                return new ProcessKeystrokeRiskStrategy();
        }
    }
}
