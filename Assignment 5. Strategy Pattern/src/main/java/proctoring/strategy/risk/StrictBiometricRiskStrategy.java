package proctoring.strategy.risk;

import proctoring.strategy.model.ExamSessionTelemetry;

public class StrictBiometricRiskStrategy implements RiskScoringStrategy {

    @Override
    public double calculateRisk(ExamSessionTelemetry telemetry) {
        if (telemetry == null) {
            return 0.0;
        }

        double score = 0.0;
        score += telemetry.getGazeAversionSeconds() * 2.5;
        score += telemetry.getFaceDisappearanceCount() * 25.0;
        score += telemetry.getAudioDecibelSpikes() * 8.0;

        return Math.min(100.0, Math.max(0.0, score));
    }

    @Override
    public String getStrategyName() {
        return "Strict Biometric & Gaze Tracking";
    }
}
