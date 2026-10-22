package proctoring.strategy.risk;

import proctoring.strategy.model.ExamSessionTelemetry;

public class ProcessKeystrokeRiskStrategy implements RiskScoringStrategy {

    @Override
    public double calculateRisk(ExamSessionTelemetry telemetry) {
        if (telemetry == null) {
            return 0.0;
        }

        double score = 0.0;
        score += telemetry.getUnauthorizedProcessesCount() * 45.0;
        score += telemetry.getCopyPasteEvents() * 15.0;
        score += telemetry.getTabSwitches() * 10.0;

        return Math.min(100.0, Math.max(0.0, score));
    }

    @Override
    public String getStrategyName() {
        return "Process & Keystroke Monitoring";
    }
}
