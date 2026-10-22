package proctoring.strategy.risk;

import proctoring.strategy.model.ExamSessionTelemetry;

public interface RiskScoringStrategy {
    double calculateRisk(ExamSessionTelemetry telemetry);
    String getStrategyName();
}
