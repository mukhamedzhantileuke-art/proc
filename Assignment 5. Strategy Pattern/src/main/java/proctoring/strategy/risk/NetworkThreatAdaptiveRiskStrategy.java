package proctoring.strategy.risk;

import proctoring.strategy.external.ThreatIntelligenceFeed;
import proctoring.strategy.model.ExamSessionTelemetry;

public class NetworkThreatAdaptiveRiskStrategy implements RiskScoringStrategy {
    private final ThreatIntelligenceFeed threatFeed;

    public NetworkThreatAdaptiveRiskStrategy(ThreatIntelligenceFeed threatFeed) {
        if (threatFeed == null) {
            throw new IllegalArgumentException("ThreatIntelligenceFeed must not be null.");
        }
        this.threatFeed = threatFeed;
    }

    @Override
    public double calculateRisk(ExamSessionTelemetry telemetry) {
        if (telemetry == null) {
            return 0.0;
        }

        double score = 0.0;
        double externalSubnetThreat = threatFeed.getSubnetThreatScore(telemetry.getClientIpAddress());
        boolean isHighRisk = threatFeed.isHighRiskNetwork(telemetry.getClientIpAddress());

        score += externalSubnetThreat;
        if (isHighRisk) {
            score += 25.0;
        }

        score += telemetry.getTabSwitches() * 8.0;
        score += telemetry.getUnauthorizedProcessesCount() * 30.0;
        score += telemetry.getFaceDisappearanceCount() * 10.0;

        return Math.min(100.0, Math.max(0.0, score));
    }

    @Override
    public String getStrategyName() {
        return "Network Threat Adaptive Strategy";
    }
}
