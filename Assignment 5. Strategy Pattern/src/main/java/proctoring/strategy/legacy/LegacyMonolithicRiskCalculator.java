package proctoring.strategy.legacy;

import proctoring.strategy.external.ThreatIntelligenceFeed;
import proctoring.strategy.model.ExamSessionTelemetry;

public class LegacyMonolithicRiskCalculator {
    private final ThreatIntelligenceFeed threatFeed;

    public LegacyMonolithicRiskCalculator(ThreatIntelligenceFeed threatFeed) {
        this.threatFeed = threatFeed;
    }

    public double calculateRisk(String algorithmType, ExamSessionTelemetry telemetry) {
        if (telemetry == null) {
            return 0.0;
        }

        switch (algorithmType.toUpperCase()) {
            case "BIOMETRIC":
                double bioScore = 0.0;
                bioScore += telemetry.getGazeAversionSeconds() * 2.5;
                bioScore += telemetry.getFaceDisappearanceCount() * 25.0;
                bioScore += telemetry.getAudioDecibelSpikes() * 8.0;
                return Math.min(100.0, Math.max(0.0, bioScore));

            case "PROCESS":
                double procScore = 0.0;
                procScore += telemetry.getUnauthorizedProcessesCount() * 45.0;
                procScore += telemetry.getCopyPasteEvents() * 15.0;
                procScore += telemetry.getTabSwitches() * 10.0;
                return Math.min(100.0, Math.max(0.0, procScore));

            case "NETWORK":
                double netScore = 0.0;
                if (threatFeed != null) {
                    netScore += threatFeed.getSubnetThreatScore(telemetry.getClientIpAddress());
                    if (threatFeed.isHighRiskNetwork(telemetry.getClientIpAddress())) {
                        netScore += 25.0;
                    }
                }
                netScore += telemetry.getTabSwitches() * 8.0;
                netScore += telemetry.getUnauthorizedProcessesCount() * 30.0;
                netScore += telemetry.getFaceDisappearanceCount() * 10.0;
                return Math.min(100.0, Math.max(0.0, netScore));

            default:
                throw new IllegalArgumentException("Unknown algorithm type: " + algorithmType);
        }
    }
}
